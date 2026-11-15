-- SkillSwap - esquema inicial.
-- Compatible con MySQL 8 y con H2 en modo MySQL (las pruebas corren estas mismas migraciones).
-- Hibernate solo valida el esquema (hibernate.hbm2ddl.auto=validate): las tablas se crean aca.

-- ---------------------------------------------------------------- Catalogo (Composite, SINGLE_TABLE)
CREATE TABLE elemento_catalogo (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    tipo      VARCHAR(31)  NOT NULL,
    nombre    VARCHAR(120) NOT NULL,
    padre_id  BIGINT       NULL,
    CONSTRAINT pk_elemento_catalogo PRIMARY KEY (id),
    CONSTRAINT fk_elemento_catalogo_padre FOREIGN KEY (padre_id) REFERENCES elemento_catalogo (id)
);
CREATE INDEX idx_elemento_catalogo_padre ON elemento_catalogo (padre_id);

-- ---------------------------------------------------------------- Politica de cancelacion (Strategy, SINGLE_TABLE)
CREATE TABLE politica_cancelacion (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    tipo                    VARCHAR(31) NOT NULL,
    nombre                  VARCHAR(60) NOT NULL,
    horas_reintegro_total   INT         NULL,
    horas_reintegro_parcial INT         NULL,
    CONSTRAINT pk_politica_cancelacion PRIMARY KEY (id)
);

-- ---------------------------------------------------------------- Perfil (baja logica: activo)
CREATE TABLE perfil (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    user_id                 VARCHAR(64)   NOT NULL,
    tipo                    VARCHAR(20)   NOT NULL,
    nombre_visible          VARCHAR(120)  NOT NULL,
    biografia               VARCHAR(2000) NULL,
    zona_horaria            VARCHAR(64)   NULL,
    reputacion              DOUBLE        NOT NULL DEFAULT 0,
    cantidad_calificaciones INT           NOT NULL DEFAULT 0,
    activo                  BOOLEAN       NOT NULL DEFAULT TRUE,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_perfil PRIMARY KEY (id),
    CONSTRAINT uk_perfil_user_id UNIQUE (user_id)
);

CREATE TABLE perfil_habilidad (
    perfil_id    BIGINT NOT NULL,
    habilidad_id BIGINT NOT NULL,
    CONSTRAINT pk_perfil_habilidad PRIMARY KEY (perfil_id, habilidad_id),
    CONSTRAINT fk_perfil_habilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id),
    CONSTRAINT fk_perfil_habilidad_habilidad FOREIGN KEY (habilidad_id) REFERENCES elemento_catalogo (id)
);

CREATE TABLE perfil_interes (
    perfil_id    BIGINT NOT NULL,
    habilidad_id BIGINT NOT NULL,
    CONSTRAINT pk_perfil_interes PRIMARY KEY (perfil_id, habilidad_id),
    CONSTRAINT fk_perfil_interes_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id),
    CONSTRAINT fk_perfil_interes_habilidad FOREIGN KEY (habilidad_id) REFERENCES elemento_catalogo (id)
);

CREATE TABLE perfil_disponibilidad (
    perfil_id BIGINT      NOT NULL,
    orden     INT         NOT NULL,
    dia       VARCHAR(10) NOT NULL,
    desde     TIME        NOT NULL,
    hasta     TIME        NOT NULL,
    CONSTRAINT pk_perfil_disponibilidad PRIMARY KEY (perfil_id, orden),
    CONSTRAINT fk_perfil_disponibilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

CREATE TABLE perfil_canal (
    perfil_id BIGINT      NOT NULL,
    canal     VARCHAR(10) NOT NULL,
    CONSTRAINT pk_perfil_canal PRIMARY KEY (perfil_id, canal),
    CONSTRAINT fk_perfil_canal_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

-- ---------------------------------------------------------------- Publicacion (State: nombre_estado, @Version)
CREATE TABLE publicacion (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    autor_id                BIGINT        NOT NULL,
    tipo                    VARCHAR(20)   NOT NULL,
    titulo                  VARCHAR(200)  NULL,
    descripcion             VARCHAR(2000) NULL,
    categoria_id            BIGINT        NULL,
    modalidad               VARCHAR(20)   NULL,
    duracion_minutos        INT           NOT NULL,
    cupo                    INT           NOT NULL,
    cupos_ocupados          INT           NOT NULL DEFAULT 0,
    precio_base             BIGINT        NULL,
    nombre_estado           VARCHAR(20)   NOT NULL,
    politica_cancelacion_id BIGINT        NULL,
    fecha_publicacion       DATETIME(6)   NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_publicacion PRIMARY KEY (id),
    CONSTRAINT fk_publicacion_autor FOREIGN KEY (autor_id) REFERENCES perfil (id),
    CONSTRAINT fk_publicacion_categoria FOREIGN KEY (categoria_id) REFERENCES elemento_catalogo (id),
    CONSTRAINT fk_publicacion_politica FOREIGN KEY (politica_cancelacion_id) REFERENCES politica_cancelacion (id),
    CONSTRAINT ck_publicacion_cupo CHECK (cupos_ocupados >= 0 AND cupos_ocupados <= cupo)
);
-- Indices de la busqueda: estado + categoria, precio y fecha (ORDER BY)
CREATE INDEX idx_publicacion_estado_categoria ON publicacion (nombre_estado, categoria_id);
CREATE INDEX idx_publicacion_precio ON publicacion (precio_base);
CREATE INDEX idx_publicacion_fecha ON publicacion (fecha_publicacion);

CREATE TABLE publicacion_habilidad (
    publicacion_id BIGINT NOT NULL,
    habilidad_id   BIGINT NOT NULL,
    CONSTRAINT pk_publicacion_habilidad PRIMARY KEY (publicacion_id, habilidad_id),
    CONSTRAINT fk_publicacion_habilidad_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT fk_publicacion_habilidad_habilidad FOREIGN KEY (habilidad_id) REFERENCES elemento_catalogo (id)
);
CREATE INDEX idx_publicacion_habilidad_habilidad ON publicacion_habilidad (habilidad_id);

CREATE TABLE publicacion_etiqueta (
    publicacion_id BIGINT      NOT NULL,
    etiqueta       VARCHAR(60) NOT NULL,
    CONSTRAINT pk_publicacion_etiqueta PRIMARY KEY (publicacion_id, etiqueta),
    CONSTRAINT fk_publicacion_etiqueta_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

CREATE TABLE franja_horaria (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT      NOT NULL,
    inicio         DATETIME(6) NOT NULL,
    fin            DATETIME(6) NOT NULL,
    ocupada        BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_franja_horaria PRIMARY KEY (id),
    CONSTRAINT fk_franja_horaria_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

CREATE TABLE promocion (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    tipo           VARCHAR(31) NOT NULL,
    publicacion_id BIGINT      NOT NULL,
    codigo         VARCHAR(40) NULL,
    vigente_desde  DATE        NOT NULL,
    vigente_hasta  DATE        NOT NULL,
    porcentaje     INT         NULL,
    monto          BIGINT      NULL,
    CONSTRAINT pk_promocion PRIMARY KEY (id),
    CONSTRAINT fk_promocion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

-- Historial inmutable: solo INSERT (orden = posicion en la lista)
CREATE TABLE publicacion_historial (
    publicacion_id  BIGINT       NOT NULL,
    orden           INT          NOT NULL,
    fecha           DATETIME(6)  NOT NULL,
    actor_id        VARCHAR(64)  NULL,
    estado_anterior VARCHAR(20)  NULL,
    estado_nuevo    VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(500) NULL,
    CONSTRAINT pk_publicacion_historial PRIMARY KEY (publicacion_id, orden),
    CONSTRAINT fk_publicacion_historial_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

-- ---------------------------------------------------------------- Contratacion (State: nombre_estado, @Version)
CREATE TABLE contratacion (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    publicacion_id           BIGINT        NOT NULL,
    cliente_id               BIGINT        NOT NULL,
    proveedor_id             BIGINT        NOT NULL,
    mensaje                  VARCHAR(1000) NULL,
    objetivo                 VARCHAR(1000) NULL,
    franja_id                BIGINT        NOT NULL,
    precio_acordado          BIGINT        NOT NULL,
    detalle_precio           VARCHAR(500)  NULL,
    nombre_estado            VARCHAR(20)   NOT NULL,
    propuesta_franja_id      BIGINT        NULL,
    propuesta_por            VARCHAR(64)   NULL,
    propuesta_fecha          DATETIME(6)   NULL,
    calificada_por_cliente   BOOLEAN       NOT NULL DEFAULT FALSE,
    calificada_por_proveedor BOOLEAN       NOT NULL DEFAULT FALSE,
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_contratacion PRIMARY KEY (id),
    CONSTRAINT fk_contratacion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT fk_contratacion_cliente FOREIGN KEY (cliente_id) REFERENCES perfil (id),
    CONSTRAINT fk_contratacion_proveedor FOREIGN KEY (proveedor_id) REFERENCES perfil (id),
    CONSTRAINT fk_contratacion_franja FOREIGN KEY (franja_id) REFERENCES franja_horaria (id),
    CONSTRAINT fk_contratacion_propuesta_franja FOREIGN KEY (propuesta_franja_id) REFERENCES franja_horaria (id)
);
CREATE INDEX idx_contratacion_cliente ON contratacion (cliente_id);
CREATE INDEX idx_contratacion_proveedor ON contratacion (proveedor_id);

CREATE TABLE contratacion_historial (
    contratacion_id BIGINT       NOT NULL,
    orden           INT          NOT NULL,
    fecha           DATETIME(6)  NOT NULL,
    actor_id        VARCHAR(64)  NULL,
    estado_anterior VARCHAR(20)  NULL,
    estado_nuevo    VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(500) NULL,
    CONSTRAINT pk_contratacion_historial PRIMARY KEY (contratacion_id, orden),
    CONSTRAINT fk_contratacion_historial_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id)
);
-- Popularidad reciente: fecha de la solicitud (orden = 0)
CREATE INDEX idx_contratacion_historial_fecha ON contratacion_historial (orden, fecha);

-- ---------------------------------------------------------------- Billetera y movimientos (@Version)
CREATE TABLE billetera (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    titular_id BIGINT NOT NULL,
    saldo      BIGINT NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_billetera PRIMARY KEY (id),
    CONSTRAINT uk_billetera_titular UNIQUE (titular_id),
    CONSTRAINT fk_billetera_titular FOREIGN KEY (titular_id) REFERENCES perfil (id),
    CONSTRAINT ck_billetera_saldo CHECK (saldo >= 0)
);

CREATE TABLE movimiento_credito (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    billetera_id    BIGINT       NOT NULL,
    fecha           DATETIME(6)  NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    monto           BIGINT       NOT NULL,
    concepto        VARCHAR(200) NULL,
    contratacion_id BIGINT       NULL,
    CONSTRAINT pk_movimiento_credito PRIMARY KEY (id),
    CONSTRAINT fk_movimiento_credito_billetera FOREIGN KEY (billetera_id) REFERENCES billetera (id),
    CONSTRAINT fk_movimiento_credito_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id),
    -- Doble cobro: un solo movimiento de cada tipo por billetera y contratacion
    CONSTRAINT uk_movimiento_doble_cobro UNIQUE (billetera_id, contratacion_id, tipo)
);

-- ---------------------------------------------------------------- Calificacion
CREATE TABLE calificacion (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    contratacion_id BIGINT        NOT NULL,
    autor_id        BIGINT        NOT NULL,
    calificado_id   BIGINT        NOT NULL,
    puntaje         INT           NOT NULL,
    comentario      VARCHAR(1000) NULL,
    fecha           DATETIME(6)   NOT NULL,
    CONSTRAINT pk_calificacion PRIMARY KEY (id),
    CONSTRAINT fk_calificacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id),
    CONSTRAINT fk_calificacion_autor FOREIGN KEY (autor_id) REFERENCES perfil (id),
    CONSTRAINT fk_calificacion_calificado FOREIGN KEY (calificado_id) REFERENCES perfil (id),
    -- Cada participante califica una sola vez cada contratacion
    CONSTRAINT uk_calificacion_contratacion_autor UNIQUE (contratacion_id, autor_id),
    CONSTRAINT ck_calificacion_puntaje CHECK (puntaje BETWEEN 1 AND 5)
);
CREATE INDEX idx_calificacion_calificado ON calificacion (calificado_id);

-- ---------------------------------------------------------------- Notificacion (bandeja interna)
CREATE TABLE notificacion (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    destinatario_id VARCHAR(64)  NOT NULL,
    canal           VARCHAR(10)  NOT NULL,
    tipo_evento     VARCHAR(30)  NOT NULL,
    mensaje         VARCHAR(500) NOT NULL,
    fecha           DATETIME(6)  NOT NULL,
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_notificacion PRIMARY KEY (id)
);
CREATE INDEX idx_notificacion_destinatario ON notificacion (destinatario_id, leida);

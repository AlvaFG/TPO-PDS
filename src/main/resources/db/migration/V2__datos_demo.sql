-- SkillSwap - datos de demostracion reproducibles (ids fijos, fechas fijas).
-- Usuario activo de la UI: user-mateo. Los userId coinciden con IdentityClientMock.USUARIOS_DEMO.
-- Los saldos son consistentes con los movimientos y la reputacion con las calificaciones.

-- ---------------------------------------------------------------- Catalogo: categorias y habilidades
INSERT INTO elemento_catalogo (id, tipo, nombre, padre_id) VALUES
    (1, 'CATEGORIA', 'Tecnología', NULL),
    (2, 'CATEGORIA', 'Desarrollo backend', 1),
    (3, 'CATEGORIA', 'Arquitectura de software', 1),
    (4, 'CATEGORIA', 'Diseño', NULL),
    (5, 'CATEGORIA', 'Creatividad', NULL),
    (6, 'CATEGORIA', 'Negocios', NULL),
    (7, 'CATEGORIA', 'Productividad', NULL);

INSERT INTO elemento_catalogo (id, tipo, nombre, padre_id) VALUES
    (10, 'HABILIDAD', 'Java', 2),
    (11, 'HABILIDAD', 'Entrevistas técnicas', 2),
    (12, 'HABILIDAD', 'Clean Architecture', 3),
    (13, 'HABILIDAD', 'SOLID', 3),
    (14, 'HABILIDAD', 'Microservicios', 3),
    (15, 'HABILIDAD', 'UX', 4),
    (16, 'HABILIDAD', 'Portfolio', 4),
    (17, 'HABILIDAD', 'Fotografía', 5),
    (18, 'HABILIDAD', 'Liderazgo', 6),
    (19, 'HABILIDAD', 'Excel', 7),
    (20, 'HABILIDAD', 'Análisis de datos', 7);

-- ---------------------------------------------------------------- Politicas de cancelacion
INSERT INTO politica_cancelacion (id, tipo, nombre, horas_reintegro_total, horas_reintegro_parcial) VALUES
    (1, 'FLEXIBLE', 'Flexible', 24, 2),
    (2, 'ESTRICTA', 'Estricta', NULL, 72);

-- ---------------------------------------------------------------- Perfiles
INSERT INTO perfil (id, user_id, tipo, nombre_visible, biografia, zona_horaria, reputacion,
                    cantidad_calificaciones, activo, version) VALUES
    (1, 'user-mateo', 'PROFESIONAL', 'Mateo Silva', 'Desarrollador Java que da mentorías a quienes empiezan.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (2, 'user-sofia', 'PROFESIONAL', 'Sofía Méndez', 'Líder técnica. Preparo entrevistas de backend.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (3, 'user-tomas', 'PROFESIONAL', 'Tomás Vidal', 'Diseñador UX con foco en portfolios.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (4, 'user-camila', 'PROFESIONAL', 'Camila Torres', 'Arquitecta de software.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (5, 'user-nicolas', 'PROFESIONAL', 'Nicolás Paz', 'Fotógrafo de producto.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (6, 'user-laura', 'PROFESIONAL', 'Laura Rossi', 'Coach de liderazgo para equipos de tecnología.', 'America/Argentina/Buenos_Aires', 5.0, 1, TRUE, 0),
    (7, 'user-martin', 'PROFESIONAL', 'Martín Aguirre', 'Analista de datos.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0),
    (8, 'user-acme', 'ORGANIZACION', 'Acme Labs', 'Organización que publica desafíos de producto.', 'America/Argentina/Buenos_Aires', 0, 0, TRUE, 0);

INSERT INTO perfil_canal (perfil_id, canal) VALUES
    (1, 'INTERNA'), (1, 'EMAIL'),
    (2, 'INTERNA'), (2, 'PUSH'),
    (3, 'INTERNA'), (4, 'INTERNA'), (5, 'INTERNA'), (6, 'INTERNA'), (6, 'EMAIL'), (7, 'INTERNA'), (8, 'INTERNA');

INSERT INTO perfil_habilidad (perfil_id, habilidad_id) VALUES
    (1, 10), (2, 10), (2, 11), (3, 15), (3, 16), (4, 12), (4, 13), (4, 14), (5, 17), (6, 18), (7, 19), (7, 20);

INSERT INTO perfil_interes (perfil_id, habilidad_id) VALUES
    (1, 12), (1, 15), (2, 18), (3, 17);

INSERT INTO perfil_disponibilidad (perfil_id, orden, dia, desde, hasta) VALUES
    (1, 0, 'MONDAY', '18:00:00', '21:00:00'),
    (1, 1, 'WEDNESDAY', '18:00:00', '21:00:00');

-- ---------------------------------------------------------------- Publicaciones
INSERT INTO publicacion (id, autor_id, tipo, titulo, descripcion, categoria_id, modalidad, duracion_minutos, cupo,
                         cupos_ocupados, precio_base, nombre_estado, politica_cancelacion_id, fecha_publicacion, version) VALUES
    (1, 2, 'MENTORIA', 'Preparación para entrevistas técnicas', 'Simulacro completo, feedback y plan personalizado para tu próxima entrevista.', 2, 'REMOTA', 60, 3, 0, 180, 'PUBLICADA', 1, '2026-11-01 10:00:00', 0),
    (2, 3, 'MENTORIA', 'Revisión profesional de portfolio', 'Mejorá la narrativa y presentación de tus proyectos para destacarte.', 4, 'REMOTA', 45, 2, 1, 120, 'PUBLICADA', 1, '2026-11-02 10:00:00', 0),
    (3, 4, 'MENTORIA', 'Arquitectura limpia aplicada', 'Sesión práctica para ordenar dependencias y casos de uso en proyectos Java.', 3, 'REMOTA', 90, 2, 0, 260, 'PUBLICADA', 2, '2026-11-03 10:00:00', 0),
    (4, 5, 'SERVICIO', 'Fotografía de producto con celular', 'Iluminación, encuadre y edición simple para catálogos y redes.', 5, 'PRESENCIAL', 75, 1, 0, 150, 'PUBLICADA', 2, '2026-11-04 10:00:00', 0),
    (5, 6, 'MENTORIA', 'Mentoría de liderazgo para equipos', 'Herramientas concretas para conversaciones, feedback y delegación.', 6, 'REMOTA', 60, 2, 1, 220, 'PUBLICADA', 1, '2026-10-20 10:00:00', 0),
    (6, 7, 'SERVICIO', 'Excel para decisiones de negocio', 'Tablas dinámicas, escenarios y un dashboard listo para reutilizar.', 7, 'REMOTA', 90, 3, 0, 140, 'PUBLICADA', 1, '2026-11-05 10:00:00', 0),
    (7, 1, 'MENTORIA', 'Mentoría de Java inicial', 'Primeros pasos con Java: sintaxis, colecciones y pruebas unitarias.', 2, 'REMOTA', 60, 2, 0, 100, 'PUBLICADA', 1, '2026-11-06 10:00:00', 0),
    (8, 1, 'SERVICIO', 'Revisión de código Java', 'Revisión de un repositorio con sugerencias de diseño y pruebas.', 2, 'REMOTA', 45, 1, 0, 90, 'BORRADOR', NULL, NULL, 0),
    (9, 8, 'DESAFIO', 'Desafío: prototipo de app de turnos', 'Buscamos un prototipo navegable para reservar turnos desde el celular.', 1, 'REMOTA', 120, 1, 0, 500, 'PUBLICADA', 2, '2026-11-07 10:00:00', 0);

INSERT INTO publicacion_habilidad (publicacion_id, habilidad_id) VALUES
    (1, 10), (1, 11), (2, 15), (2, 16), (3, 10), (3, 12), (3, 13), (4, 17), (5, 18), (6, 19), (6, 20), (7, 10), (8, 10), (9, 15);

INSERT INTO publicacion_etiqueta (publicacion_id, etiqueta) VALUES
    (1, 'Java'), (1, 'Entrevistas'), (1, 'Backend'),
    (2, 'UX'), (2, 'Portfolio'), (2, 'Carrera'),
    (3, 'Clean Architecture'), (3, 'SOLID'), (3, 'Java'),
    (4, 'Fotografía'), (4, 'Contenido'),
    (5, 'Liderazgo'), (5, 'Equipos'),
    (6, 'Excel'), (6, 'Datos'),
    (7, 'Java'), (7, 'Inicial'),
    (8, 'Java'), (8, 'Code review'),
    (9, 'Prototipo'), (9, 'Mobile');

INSERT INTO franja_horaria (id, publicacion_id, inicio, fin, ocupada) VALUES
    (1, 1, '2027-03-01 18:00:00', '2027-03-01 19:00:00', FALSE),
    (2, 1, '2027-03-08 18:00:00', '2027-03-08 19:00:00', FALSE),
    (3, 1, '2027-03-15 18:00:00', '2027-03-15 19:00:00', FALSE),
    (4, 2, '2027-03-03 18:00:00', '2027-03-03 18:45:00', TRUE),
    (5, 2, '2027-03-10 18:00:00', '2027-03-10 18:45:00', FALSE),
    (6, 3, '2027-03-04 19:00:00', '2027-03-04 20:30:00', FALSE),
    (7, 3, '2027-03-11 19:00:00', '2027-03-11 20:30:00', FALSE),
    (8, 4, '2027-03-06 10:00:00', '2027-03-06 11:15:00', FALSE),
    (9, 4, '2027-03-13 10:00:00', '2027-03-13 11:15:00', FALSE),
    (10, 5, '2026-11-10 18:00:00', '2026-11-10 19:00:00', TRUE),
    (11, 5, '2027-03-02 18:00:00', '2027-03-02 19:00:00', FALSE),
    (12, 6, '2027-03-05 18:00:00', '2027-03-05 19:30:00', FALSE),
    (13, 6, '2027-03-12 18:00:00', '2027-03-12 19:30:00', FALSE),
    (14, 7, '2027-03-02 19:00:00', '2027-03-02 20:00:00', FALSE),
    (15, 7, '2027-03-09 19:00:00', '2027-03-09 20:00:00', FALSE),
    (16, 8, '2027-03-16 19:00:00', '2027-03-16 19:45:00', FALSE),
    (17, 9, '2027-03-20 10:00:00', '2027-03-20 12:00:00', FALSE);

INSERT INTO promocion (id, tipo, publicacion_id, codigo, vigente_desde, vigente_hasta, porcentaje, monto) VALUES
    (1, 'PORCENTUAL', 1, 'LANZAMIENTO10', '2026-11-01', '2027-06-30', 10, NULL),
    (2, 'MONTO_FIJO', 6, 'EXCEL20', '2026-11-01', '2027-06-30', NULL, 20);

INSERT INTO publicacion_historial (publicacion_id, orden, fecha, actor_id, estado_anterior, estado_nuevo, motivo) VALUES
    (1, 0, '2026-11-01 10:00:00', 'user-sofia', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (2, 0, '2026-11-02 10:00:00', 'user-tomas', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (3, 0, '2026-11-03 10:00:00', 'user-camila', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (4, 0, '2026-11-04 10:00:00', 'user-nicolas', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (5, 0, '2026-10-20 10:00:00', 'user-laura', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (6, 0, '2026-11-05 10:00:00', 'user-martin', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (7, 0, '2026-11-06 10:00:00', 'user-mateo', 'BORRADOR', 'PUBLICADA', 'Publicada'),
    (9, 0, '2026-11-07 10:00:00', 'user-acme', 'BORRADOR', 'PUBLICADA', 'Publicada');

-- ---------------------------------------------------------------- Contrataciones
-- 1: Mateo contrato a Tomas y pago (CONFIRMADA). 2: Mateo con Laura, completada y calificada por Mateo.
-- 3: Sofia solicito la mentoria de Mateo (PENDIENTE: Mateo puede aceptarla o rechazarla en la demo).
INSERT INTO contratacion (id, publicacion_id, cliente_id, proveedor_id, mensaje, objetivo, franja_id, precio_acordado,
                          detalle_precio, nombre_estado, calificada_por_cliente, calificada_por_proveedor, version) VALUES
    (1, 2, 1, 3, 'Hola Tomás, quiero mejorar mi portfolio.', 'Conseguir entrevistas', 4, 120, 'Precio base 120 SC', 'CONFIRMADA', FALSE, FALSE, 0),
    (2, 5, 1, 6, 'Me sumo a un equipo nuevo como líder.', 'Dar feedback mejor', 10, 220, 'Precio base 220 SC', 'COMPLETADA', TRUE, FALSE, 0),
    (3, 7, 2, 1, 'Quiero repasar Java antes de una entrevista.', 'Repasar colecciones', 14, 100, 'Precio base 100 SC', 'PENDIENTE', FALSE, FALSE, 0);

INSERT INTO contratacion_historial (contratacion_id, orden, fecha, actor_id, estado_anterior, estado_nuevo, motivo) VALUES
    (1, 0, '2026-11-08 10:00:00', 'user-mateo', NULL, 'PENDIENTE', 'Solicitud'),
    (1, 1, '2026-11-08 12:00:00', 'user-tomas', 'PENDIENTE', 'ACEPTADA', 'Aceptada'),
    (1, 2, '2026-11-08 13:00:00', 'user-mateo', 'ACEPTADA', 'CONFIRMADA', 'Pago confirmado'),
    (2, 0, '2026-11-01 09:00:00', 'user-mateo', NULL, 'PENDIENTE', 'Solicitud'),
    (2, 1, '2026-11-01 15:00:00', 'user-laura', 'PENDIENTE', 'ACEPTADA', 'Aceptada'),
    (2, 2, '2026-11-02 10:00:00', 'user-mateo', 'ACEPTADA', 'CONFIRMADA', 'Pago confirmado'),
    (2, 3, '2026-11-10 18:00:00', 'user-laura', 'CONFIRMADA', 'EN_CURSO', 'Sesion iniciada'),
    (2, 4, '2026-11-10 19:05:00', 'user-laura', 'EN_CURSO', 'COMPLETADA', 'Sesion completada'),
    (3, 0, '2026-11-14 09:30:00', 'user-sofia', NULL, 'PENDIENTE', 'Solicitud');

INSERT INTO calificacion (id, contratacion_id, autor_id, calificado_id, puntaje, comentario, fecha) VALUES
    (1, 2, 1, 6, 5, 'Muy clara y práctica.', '2026-11-11 10:00:00');

-- ---------------------------------------------------------------- Billeteras y movimientos
INSERT INTO billetera (id, titular_id, saldo, version) VALUES
    (1, 1, 2160, 0),
    (2, 2, 1000, 0),
    (3, 3, 1120, 0),
    (4, 4, 1000, 0),
    (5, 5, 1000, 0),
    (6, 6, 1220, 0),
    (7, 7, 1000, 0),
    (8, 8, 3000, 0);

INSERT INTO movimiento_credito (id, billetera_id, fecha, tipo, monto, concepto, contratacion_id) VALUES
    (1, 1, '2026-10-15 09:00:00', 'CREDITO', 2500, 'Saldo inicial de bienvenida', NULL),
    (2, 2, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (3, 3, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (4, 4, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (5, 5, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (6, 6, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (7, 7, '2026-10-15 09:00:00', 'CREDITO', 1000, 'Saldo inicial de bienvenida', NULL),
    (8, 8, '2026-10-15 09:00:00', 'CREDITO', 3000, 'Saldo inicial de bienvenida', NULL),
    (9, 1, '2026-11-02 10:00:00', 'DEBITO', 220, 'Pago contratacion 2', 2),
    (10, 6, '2026-11-02 10:00:00', 'CREDITO', 220, 'Pago contratacion 2', 2),
    (11, 1, '2026-11-08 13:00:00', 'DEBITO', 120, 'Pago contratacion 1', 1),
    (12, 3, '2026-11-08 13:00:00', 'CREDITO', 120, 'Pago contratacion 1', 1);

-- ---------------------------------------------------------------- Notificaciones (bandeja interna)
INSERT INTO notificacion (id, destinatario_id, canal, tipo_evento, mensaje, fecha, leida) VALUES
    (1, 'user-mateo', 'INTERNA', 'SOLICITUD_ACEPTADA', 'Contratacion 1: solicitud aceptada', '2026-11-08 12:00:00', TRUE),
    (2, 'user-mateo', 'INTERNA', 'PAGO_CONFIRMADO', 'Contratacion 1: pago confirmado', '2026-11-08 13:00:00', FALSE),
    (3, 'user-mateo', 'INTERNA', 'SESION_COMPLETADA', 'Contratacion 2: sesion completada', '2026-11-10 19:05:00', TRUE),
    (4, 'user-mateo', 'INTERNA', 'SOLICITUD_RECIBIDA', 'Contratacion 3: solicitud recibida', '2026-11-14 09:30:00', FALSE),
    (5, 'user-tomas', 'INTERNA', 'PAGO_CONFIRMADO', 'Contratacion 1: pago confirmado', '2026-11-08 13:00:00', FALSE),
    (6, 'user-laura', 'INTERNA', 'SESION_COMPLETADA', 'Contratacion 2: sesion completada', '2026-11-10 19:05:00', FALSE);

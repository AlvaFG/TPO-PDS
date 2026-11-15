package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalInterno;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalNotificacionFactory;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentidadAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentityClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.PushAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.PushClientMock;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/* Adapters contra sus clientes mock (traduccion de parametros) y la Simple Factory de canales. */
class AdaptadoresTest {

    private final IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock());
    private final EmailClientMock email = new EmailClientMock();
    private final PushClientMock push = new PushClientMock();
    private final Repositorios.Notificaciones bandeja = new Repositorios.Notificaciones();

    private static Notificacion notificacion(String destinatario, CanalNotificacion canal) {
        return new Notificacion(destinatario, canal, TipoEvento.PAGO_CONFIRMADO, "Contratacion 7: pago confirmado",
                LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    @Test
    void identidadReconoceALosUsuariosDeLaDemoYRechazaAlResto() {
        assertTrue(identidad.validarUsuario("user-mateo"));
        assertTrue(IdentityClientMock.USUARIOS_DEMO.keySet().stream().allMatch(identidad::validarUsuario));
        assertFalse(identidad.validarUsuario("desconocido"));
        assertFalse(identidad.validarUsuario(null));
        assertEquals("mateo.silva@skillswap.test", identidad.obtenerEmail("user-mateo"));
        assertNull(identidad.obtenerEmail("desconocido"));
    }

    @Test
    void identidadPuedeConfigurarseConOtrosUsuarios() {
        IdentidadAdapter otra = new IdentidadAdapter(new IdentityClientMock(Map.of("ana", "ana@test")));
        assertTrue(otra.validarUsuario("ana"));
        assertFalse(otra.validarUsuario("user-mateo"));
    }

    @Test
    void emailTraduceLaNotificacionASendMailConElEmailDeIdentidad() {
        new EmailAdapter(email, identidad).enviar(notificacion("user-laura", CanalNotificacion.EMAIL));

        EmailClientMock.Correo correo = email.getEnviados().get(0);
        assertEquals("laura.rossi@skillswap.test", correo.to());
        assertEquals("SkillSwap - pago confirmado", correo.subject());
        assertEquals("Contratacion 7: pago confirmado", correo.body());
    }

    @Test
    void sinEmailConocidoNoSeEnviaElCorreo() {
        new EmailAdapter(email, identidad).enviar(notificacion("desconocido", CanalNotificacion.EMAIL));
        assertTrue(email.getEnviados().isEmpty());
    }

    @Test
    void pushTraduceLaNotificacionAPushConDispositivoYPayload() {
        new PushAdapter(push).enviar(notificacion("user-sofia", CanalNotificacion.PUSH));

        PushClientMock.Push enviado = push.getEnviados().get(0);
        assertEquals("device-user-sofia", enviado.deviceId());
        assertEquals("{\"evento\":\"PAGO_CONFIRMADO\",\"mensaje\":\"Contratacion 7: pago confirmado\"}", enviado.payload());
    }

    @Test
    void elCanalInternoGuardaEnLaBandeja() {
        new CanalInterno(bandeja).enviar(notificacion("user-mateo", CanalNotificacion.INTERNA));
        assertEquals(1, bandeja.listarPorDestinatario("user-mateo", 0, 10).totalElementos());
    }

    @Test
    void laFactoryCreaElCanalDeCadaPreferencia() {
        CanalNotificacionFactory factory = new CanalNotificacionFactory(email, push, identidad, bandeja);
        assertInstanceOf(EmailAdapter.class, factory.crear(CanalNotificacion.EMAIL));
        assertInstanceOf(PushAdapter.class, factory.crear(CanalNotificacion.PUSH));
        assertInstanceOf(CanalInterno.class, factory.crear(CanalNotificacion.INTERNA));
    }
}

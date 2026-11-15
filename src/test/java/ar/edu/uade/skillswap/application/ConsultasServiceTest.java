package ar.edu.uade.skillswap.application;

import ar.edu.uade.skillswap.application.dto.BilleteraDto;
import ar.edu.uade.skillswap.application.dto.NotificacionDto;
import ar.edu.uade.skillswap.application.dto.Pagina;
import ar.edu.uade.skillswap.application.exception.AccesoDenegadoException;
import ar.edu.uade.skillswap.application.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.application.service.BilleteraService;
import ar.edu.uade.skillswap.application.service.NotificacionService;
import ar.edu.uade.skillswap.domain.model.Billetera;
import ar.edu.uade.skillswap.domain.model.CanalNotificacion;
import ar.edu.uade.skillswap.domain.model.Notificacion;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.TipoEvento;
import ar.edu.uade.skillswap.domain.model.TipoMovimiento;
import ar.edu.uade.skillswap.soporte.Datos;
import ar.edu.uade.skillswap.soporte.Repositorios;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.uade.skillswap.soporte.Datos.INICIO;
import static org.junit.jupiter.api.Assertions.*;

/* Bandeja de notificaciones, billetera y paginacion. */
class ConsultasServiceTest {

    @Test
    void bandejaPaginadaYMarcarLeida() {
        Repositorios.Notificaciones repo = new Repositorios.Notificaciones();
        NotificacionService service = new NotificacionService(repo);
        Notificacion n = repo.guardar(new Notificacion("cli", CanalNotificacion.INTERNA, TipoEvento.PAGO_CONFIRMADO, "Pago", INICIO));
        repo.guardar(new Notificacion("cli", CanalNotificacion.INTERNA, TipoEvento.CANCELACION, "Cancelada", INICIO));
        repo.guardar(new Notificacion("otro", CanalNotificacion.INTERNA, TipoEvento.CANCELACION, "Ajena", INICIO));

        Pagina<NotificacionDto> pagina = service.listar("cli", 0, 1);
        assertEquals(2, pagina.totalElementos());
        assertEquals("PAGO_CONFIRMADO", pagina.contenido().get(0).tipo());
        assertFalse(pagina.contenido().get(0).leida());

        service.marcarLeida("cli", n.getId());
        assertTrue(n.isLeida());
        assertThrows(AccesoDenegadoException.class, () -> service.marcarLeida("otro", n.getId()));
        assertThrows(RecursoNoEncontradoException.class, () -> service.marcarLeida("cli", 99L));
    }

    @Test
    void billeteraConSaldoYMovimientos() {
        Repositorios.Billeteras repo = new Repositorios.Billeteras();
        Billetera b = repo.guardar(new Billetera(Datos.profesional("cli"), new SkillCoins(100)));
        b.acreditar(new SkillCoins(50), TipoMovimiento.REINTEGRO, "Reintegro", 1L);
        BilleteraService service = new BilleteraService(repo);

        BilleteraDto dto = service.consultar("cli");
        assertEquals(150, dto.saldo());
        assertEquals("REINTEGRO", dto.movimientos().get(0).tipo());
        assertEquals(50, dto.movimientos().get(0).monto());
        assertEquals("Reintegro", dto.movimientos().get(0).concepto());
        assertThrows(RecursoNoEncontradoException.class, () -> service.consultar("nadie"));
    }

    @Test
    void acreditarDemoSumaSaldoYRegistraMovimiento() {
        Repositorios.Billeteras repo = new Repositorios.Billeteras();
        repo.guardar(new Billetera(Datos.profesional("cli"), new SkillCoins(100)));
        BilleteraService service = new BilleteraService(repo);

        BilleteraDto dto = service.acreditarDemo("cli", 40);
        assertEquals(140, dto.saldo());
        assertEquals("CREDITO", dto.movimientos().get(0).tipo());
        assertThrows(RecursoNoEncontradoException.class, () -> service.acreditarDemo("nadie", 10));
    }

    @Test
    void pagina() {
        Pagina<Integer> pagina = Pagina.de(List.of(1, 2, 3, 4, 5), 1, 2);
        assertEquals(List.of(3, 4), pagina.contenido());
        assertEquals(3, pagina.totalPaginas());
        assertEquals(List.of("6", "8"), pagina.map(i -> String.valueOf(i * 2)).contenido());
        assertTrue(Pagina.de(List.of(1), 5, 2).contenido().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Pagina.de(List.of(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Pagina.de(List.of(), -1, 1));
    }
}

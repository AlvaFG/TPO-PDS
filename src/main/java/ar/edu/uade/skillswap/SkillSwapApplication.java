package ar.edu.uade.skillswap;

import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.infrastructure.adapter.CanalNotificacionFactory;
import ar.edu.uade.skillswap.infrastructure.adapter.EmailClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentidadAdapter;
import ar.edu.uade.skillswap.infrastructure.adapter.IdentityClientMock;
import ar.edu.uade.skillswap.infrastructure.adapter.PushClientMock;
import ar.edu.uade.skillswap.infrastructure.persistence.BilleteraJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CalificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.CatalogoJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.ContratacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaGestorTransacciones;
import ar.edu.uade.skillswap.infrastructure.persistence.NotificacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PerfilJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PersistenceFactory;
import ar.edu.uade.skillswap.infrastructure.persistence.PoliticaCancelacionJpaRepository;
import ar.edu.uade.skillswap.infrastructure.persistence.PublicacionJpaRepository;
import ar.edu.uade.skillswap.mock.MockSkillSwapControllers;
import ar.edu.uade.skillswap.ui.SkillSwapView;
import ar.edu.uade.skillswap.ui.Theme;

import jakarta.persistence.EntityManagerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Clock;
import java.util.Arrays;

/*
 * Punto de entrada. Sin parametros usa los datos en memoria del kit; con --real usa los casos de uso sobre
 * MySQL (SKILLSWAP_DB_URL / SKILLSWAP_DB_USER / SKILLSWAP_DB_PASSWORD, por defecto el de docker-compose.yml).
 * --screenshot <archivo.png> guarda una captura y sale.
 */
public final class SkillSwapApplication {
    private SkillSwapApplication() {}

    /* Las fuentes del kit son fijas y chicas: se agranda toda la interfaz. Se puede pisar con -Dsun.java2d.uiScale=... */
    private static final String ESCALA_UI = "1.5";

    public static void main(String[] args) throws Exception {
        if (System.getProperty("sun.java2d.uiScale") == null) System.setProperty("sun.java2d.uiScale", ESCALA_UI);
        boolean real = Arrays.asList(args).contains("--real");
        String[] resto = Arrays.stream(args).filter(a -> !a.equals("--real")).toArray(String[]::new);
        Theme.install();
        EntityManagerFactory emf = real ? PersistenceFactory.crear() : null;
        UiControllers controllers = real ? controllersReales(emf) : new MockSkillSwapControllers();
        if (resto.length == 2 && "--screenshot".equals(resto[0])) {
            SkillSwapView view = new SkillSwapView(controllers);
            view.setSize(1365, 768); view.doLayout(); layoutTree(view);
            BufferedImage image = new BufferedImage(1365,768,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);view.printAll(g);g.dispose();
            ImageIO.write(image,"png",new File(resto[1]));
            if (emf != null) emf.close();
            return;
        }
        mostrarErroresEnVentana();
        SwingUtilities.invokeLater(() -> {
            JFrame frame=new JFrame(real ? "SkillSwap" : "SkillSwap · Mock UI Kit");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            if (emf != null) frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { emf.close(); }
            });
            frame.setMinimumSize(new Dimension(1120,700));frame.setSize(1365,820);frame.setLocationRelativeTo(null);
            frame.setContentPane(new SkillSwapView(controllers));frame.setVisible(true);
        });
    }

    /* La vista del kit no captura excepciones: los errores de aplicacion se muestran en un dialogo. */
    private static void mostrarErroresEnVentana() {
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            error.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, error.getMessage(),
                        "SkillSwap", JOptionPane.WARNING_MESSAGE));
        });
    }

    /* Composition Root del modo real: repositorios JPA, adaptadores mock de identidad y canales. */
    private static UiControllers controllersReales(EntityManagerFactory emf) {
        JpaGestorTransacciones gestor = new JpaGestorTransacciones(emf);
        IdentidadAdapter identidad = new IdentidadAdapter(new IdentityClientMock());
        NotificacionJpaRepository notificaciones = new NotificacionJpaRepository(gestor);
        return ConfiguracionApp.crear(new ConfiguracionApp.Puertos(
                new PerfilJpaRepository(gestor), new PublicacionJpaRepository(gestor),
                new ContratacionJpaRepository(gestor), new BilleteraJpaRepository(gestor), notificaciones,
                new CalificacionJpaRepository(gestor), new CatalogoJpaRepository(gestor),
                new PoliticaCancelacionJpaRepository(gestor), gestor, identidad,
                new CanalNotificacionFactory(new EmailClientMock(), new PushClientMock(), identidad, notificaciones),
                Clock.systemDefaultZone()));
    }

    private static void layoutTree(Container c) {
        c.doLayout();
        for(Component x:c.getComponents()) if(x instanceof Container child) layoutTree(child);
    }
}

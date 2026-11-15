package ar.edu.uade.skillswap.application.port;

import ar.edu.uade.skillswap.domain.model.CanalNotificacion;

/*
 * Puerto de la Simple Factory de canales: Application no conoce los adapters concretos.
 */
public interface ICanalNotificacionFactory {

    ICanalNotificacion crear(CanalNotificacion canal);
}

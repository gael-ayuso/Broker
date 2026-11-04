EJEMPLOS JSON - SISTEMA DE PEDIDOS CON BROKER

Este paquete contiene mensajes de referencia para comprobar el cumplimiento
del protocolo común de la actividad.

Carpetas:
- broker/: registro, listado y búsqueda de servicios.
- pedidos/: recorrido Cliente -> Broker -> Servidor para registrar, consultar
  y listar pedidos.
- errores/: ejemplos de respuestas de error.

Reglas importantes:
- Codificación UTF-8.
- Cada mensaje JSON se transmite en una sola línea y termina con \n.
- Los archivos están indentados únicamente para facilitar su lectura.
  Al enviarlos por socket, el objeto JSON puede serializarse sin indentación,
  pero debe conservar exactamente su estructura, nombres de campos y tipos.
- Los valores numéricos se envían como números JSON.
- El cliente únicamente conoce la dirección del Broker.
- El Broker transforma la petición "ejecutar" en la solicitud del servicio
  correspondiente al servidor.
- Los identificadores de pedido mostrados son ejemplos. En ejecución debe
  utilizarse el identificador realmente asignado.

Ejemplo de ejecución:
Cliente -> Broker:
  pedidos/05_ejecutar_consultarPedido_cliente_broker.json

Broker -> Servidor:
  pedidos/06_consultarPedido_broker_servidor.json

Servidor -> Broker:
  pedidos/07_consultarPedido_servidor_broker_ok.json

Broker -> Cliente:
  pedidos/08_ejecutar_consultarPedido_broker_cliente_ok.json

================================================================================
SISTEMA DE PROCESAMIENTO DE PEDIDOS DISTRIBUIDO MEDIANTE BROKER
================================================================================

1. REQUISITOS PREVIOS
--------------------------------------------------------------------------------
- Java Development Kit (JDK) 21 o superior
- Apache Maven 3.8 o superior

2. COMPILACIÓN DEL PROYECTO
--------------------------------------------------------------------------------
Desde la terminal, situarse en la raíz del proyecto (donde se ubica el archivo
pom.xml) y ejecutar:

    mvn clean compile

Este comando descargará las dependencias necesarias (Jackson y Gson) y
compilará todo el código fuente en la carpeta target/classes.

3. EJECUCIÓN DE LOS COMPONENTES
--------------------------------------------------------------------------------
Los componentes deben levantarse en terminales independientes y estrictamente
en el siguiente orden:

PASO 1: LEVANTAR EL BROKER
----------------------------------------
El Broker escucha solicitudes de registro y ejecución.
Indicar como parámetro el puerto de escucha (ejemplo: 9000).

Comando con Maven:
    mvn exec:java -Dexec.mainClass="broker.Broker" -Dexec.args="9000"

Comando con Java (desde la raíz):
    java -cp target/classes:$(mvn dependency:build-classpath | grep -A 1 'Dependencies classpath:' | tail -n 1) broker.Broker 9000

Salida esperada:
    Broker iniciado. Escuchando en el puerto: 9000

PASO 2: LEVANTAR EL SERVIDOR DE PEDIDOS
----------------------------------------
El servidor proporciona los servicios de pedidos.
Parámetros requeridos:
  <puertoServidor> <ipBroker:puertoBroker>
Ejemplo:
  9001 127.0.0.1:9000

Al arrancar, el servidor registrará automáticamente ante el Broker los servicios:
- registrarPedido
- consultarPedido
- listarPedidos

Comando con Maven:
    mvn exec:java -Dexec.mainClass="servers.Servidor" -Dexec.args="9001 127.0.0.1:9000"

Comando con Java (desde la raíz):
    java -cp target/classes:$(mvn dependency:build-classpath | grep -A 1 'Dependencies classpath:' | tail -n 1) servers.Servidor 9001 127.0.0.1:9000

Salida esperada:
    [BrokerRegistrar] Servicio 'registrarPedido' registrado correctamente...
    [BrokerRegistrar] Servicio 'consultarPedido' registrado correctamente...
    [BrokerRegistrar] Servicio 'listarPedidos' registrado correctamente...
    Servidor de Pedidos iniciado en el puerto: 9001


PASO 3: LEVANTAR EL CLIENTE
----------------------------------------
El cliente proporciona la interfaz de usuario interactiva por consola.
Únicamente debe conocer y recibir como parámetro la dirección del Broker.
Parámetro requerido:
  <ipBroker:puertoBroker>
Ejemplo:
  127.0.0.1:9000

Comando con Maven:
    mvn exec:java -Dexec.mainClass="client.Client" -Dexec.args="127.0.0.1:9000"

Comando con Java (desde la raíz):
    java -cp target/classes:$(mvn dependency:build-classpath | grep -A 1 'Dependencies classpath:' | tail -n 1) client.Client 127.0.0.1:9000

Salida esperada:
    CLIENTE DE PEDIDOS (BROKER)
    ----------------------------------------
    Broker: 127.0.0.1:9000
    ----------------------------------------
    MENU PRINCIPAL
    1. Listar servicios disponibles en el Broker
    2. Buscar servicio en el Broker
    3. Salir

4. PROTOCOLO Y COMUNICACIÓN
--------------------------------------------------------------------------------
- Comunicación mediante Sockets TCP.
- Codificación UTF-8.
- Mensajes JSON delimitados por salto de línea (\n).
- El cliente no conoce la dirección IP ni el puerto del servidor.
- El Broker localiza al servidor correspondiente y redirige las solicitudes.

5. ESTRUCTURA DE LA ENTREGA
--------------------------------------------------------------------------------
- src/          : Código fuente completo de Broker, Servidor y Cliente.
- pom.xml       : Configuración de dependencias (Jackson, Gson, JUnit).
- diagramas/    : Diagramas UML de Componentes y Secuencia en PDF.
- evidencias/    : Capturas de pantalla de la ejecución y casos de prueba
- README.txt    : Instrucciones de compilación y ejecución.

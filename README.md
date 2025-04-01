# HeatSync

Uma aplicação desktop Java para monitorar temperaturas do sistema e controlar dispositivos de resfriamento externos via Bluetooth.

## Recursos

- Monitoramento de temperatura em tempo real para:
  - CPU
  - GPU
  - Disco
- Monitoramento do consumo de energia:
  - Energia da CPU
  - Energia da GPU
  - Energia total do sistema
- Conectividade Bluetooth (SPP - Serial Port Profile):
  - Descoberta de dispositivos
  - Gerenciamento de conexão de dispositivos
  - Controle de ventilador baseado nas temperaturas do sistema (via comandos SPP)
- Modos de controle automático e manual do ventilador
- Interface gráfica Swing simples e intuitiva

## Requisitos para Execução

- Java Runtime Environment (JRE) 17 ou superior.
  - Para verificar, abra um terminal/prompt e digite: `java -version`
  - Pode ser baixado em [Adoptium (Eclipse Temurin)](https://adoptium.net/).
- Adaptador Bluetooth (Hardware).
- Sistema Operacional: Linux ou Windows.

### Dependências e Configuração do Sistema Operacional

- **Linux:**
  - É necessário ter `bluez` e `libbluetooth-dev` instalados.
  ```bash
  # Comando para instalar dependências no Debian/Ubuntu e derivados:
  sudo apt-get update && sudo apt-get install -y bluetooth bluez libbluetooth-dev
  ```
  - O serviço Bluetooth precisa estar ativo e iniciado.
  ```bash
  sudo systemctl enable bluetooth  # Garante que inicie com o sistema
  sudo systemctl start bluetooth   # Inicia o serviço imediatamente
  ```
  - O usuário que executará a aplicação precisa pertencer ao grupo `bluetooth`.
  ```bash
  sudo usermod -a -G bluetooth $USER
  # IMPORTANTE: Após adicionar o usuário ao grupo, é necessário fazer logout e login novamente no linux!
  ```

- **Windows:**
  - Certifique-se que o Bluetooth está habilitado nas configurações do Windows.
  - Verifique se os drivers do adaptador Bluetooth estão instalados corretamente (geralmente via Windows Update ou do fabricante).
  - A stack Bluetooth nativa do Windows será utilizada.

- **Nota sobre Monitoramento de Hardware:** A biblioteca JSensors usada para monitorar temperatura e energia funciona de forma mais completa e confiável no Linux. A funcionalidade pode ser limitada ou inexistente no Windows. A parte de Bluetooth deve funcionar em ambos.

## Executando a Aplicação (Usuário Final com JAR)

Esta seção é para usuários que receberam o arquivo `.jar` executável.

1.  **Verifique os Requisitos:** Certifique-se de que os requisitos de Java e do sistema operacional (incluindo dependências Bluetooth para Linux) estão atendidos conforme descrito na seção "Requisitos para Execução".
2.  **Execute o JAR:** Abra um terminal (Linux) ou Prompt de Comando/PowerShell (Windows), navegue até o diretório onde salvou o JAR e execute o comando:
    ```bash
    java -jar heatSyncJava-1.0-SNAPSHOT.jar
    ```
    *(Substitua `heatSyncJava-1.0-SNAPSHOT.jar` pelo nome exato do arquivo JAR)*

## Desenvolvimento e Compilação

Esta seção é para desenvolvedores que desejam compilar o código fonte.

1.  **Requisitos Adicionais:**
    -   Git
    -   Maven 3.6 ou superior
    -   Java Development Kit (JDK) 17 ou superior (não apenas o JRE).

2.  **Obtenha o Código Fonte:**
    ```bash
    # Substitua 'yourusername/heatSyncJava.git' pelo URL real do repositório
    git clone https://github.com/yourusername/heatSyncJava.git
    cd heatSyncJava
    ```

3.  **Compile o Projeto:**
    Este comando compila o código fonte e baixa as dependências.
    ```bash
    mvn clean compile
    ```

4.  **Executando Durante o Desenvolvimento:**
    Após compilar, você pode rodar a aplicação diretamente usando o plugin `exec-maven-plugin`:
    ```bash
    mvn exec:java -Dexec.mainClass="com.heatsync.HeatSyncApp"
    ```
    *Nota: Certifique-se que as dependências de sistema (Bluetooth) estão configuradas conforme a seção "Requisitos para Execução".*

5.  **Criando o JAR Executável (Fat JAR):**
    Para criar o arquivo `.jar` único e distribuível (que inclui todas as dependências), use o comando `package`. O `maven-shade-plugin` configurado no `pom.xml` cuidará de empacotar tudo.
    ```bash
    mvn clean package
    ```
    O JAR executável será gerado no diretório `target/` (por exemplo, `target/heatSyncJava-1.0-SNAPSHOT.jar`). Este é o arquivo que você distribuiria para usuários finais, que seguiriam as instruções da seção "Executando a Aplicação (Usuário Final com JAR)".

## Estrutura do Projeto

```
heatSyncJava/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── heatsync/ # Código fonte principal
│   │   └── resources/        # Arquivos de recursos (ex: logback.xml se existir)
│   └── test/                 # Código de teste
├── target/                   # Diretório de saída da compilação (contém o JAR)
├── pom.xml                   # Arquivo de configuração do Maven
└── README.md                 # Este arquivo
```

## Arquitetura

A aplicação segue uma arquitetura modular:

1.  **Camada de Apresentação** (`ui` package)
2.  **Camada de Controle** (`controller` package)
3.  **Camada de Serviço** (`service` package)
4.  **Módulo Bluetooth** (`service.bluetooth` package) - Usa a biblioteca BlueCove para comunicação.

## Dependências Principais (Gerenciadas pelo Maven)

-   **JSensors (2.0.0)** - Monitoramento de hardware (principalmente Linux).
-   **BlueCove (2.1.0)** - Biblioteca Bluetooth multiplataforma (SPP).
-   **SLF4J/Logback** - Logging.
-   **JUnit (4.13.2)** - Testes.
-   **JPowerMonitor (1.2.1)** - Monitoramento de energia.
-   **Swing** - GUI (Integrado ao JDK).

## Licença

Este projeto é licenciado sob a Licença MIT.

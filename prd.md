# 📄 PRD – HeatSync App (Java Swing + Bluetooth Serial + Jsensors)

## 🧩 Visão Geral

**HeatSync** é uma aplicação **desktop** desenvolvida em **Java com Swing**, responsável por monitorar e exibir dados térmicos de notebooks (CPU, GPU e disco), e por se comunicar com um **dispositivo externo via Bluetooth**, utilizando o perfil **SPP (Serial Port Profile)** para troca de dados.

A aplicação coleta periodicamente as temperaturas do sistema por meio da biblioteca **Jsensors**, e envia essas informações ao dispositivo através de uma conexão **serial Bluetooth**. Essa comunicação permite controlar a velocidade de uma ventoinha conectada a uma **base refrigeradora externa**. O dispositivo pode também enviar de volta informações como a **RPM da fan**, ou confirmações de comandos.

O sistema oferece uma **interface gráfica local (GUI)** para o usuário monitorar os dados em tempo real e controlar o sistema conforme necessário.

---

## ✅ Funcionalidades por Prioridade

### 🔧 1. Backend (Lógica do Sistema)
- [X] Criar estrutura do projeto Java com GUI em Swing
- [X] Integrar biblioteca Jsensors para leitura:
  - [X] Temperatura da CPU
  - [X] Temperatura da GPU
  - [X] Temperatura do disco
  - [X] (Opcional) Leitura de Watts da CPU/GPU (se suportado)
- [X] Criar serviço de coleta periódica (ex: `Timer` a cada 2s)
- [ ] Estabelecer conexão serial com o dispositivo via Bluetooth
- [ ] Enviar dados de temperatura pela conexão serial
- [ ] Receber dados da fan (ex: RPM) pela conexão serial

---

### 🌐 2. Interface Gráfica (Swing)
- [X] Criar janela principal com painel de monitoramento
- [ ] Exibir:
  - [X] Temperaturas (CPU, GPU, Disco)
  - [ ] RPM da fan
- [ ] Controles do usuário:
  - [X] Alternar entre modo automático e manual
  - [X] Controle deslizante de PWM (modo manual)
  - [X] Exibir status da conexão Bluetooth
- [X] Log de comunicação (console na interface ou log.txt)

---

## 🧪 Testes e Validações
- [X] Testar leitura térmica no console
- [ ] Validar envio de dados ao dispositivo via Bluetooth serial
- [ ] Validar recepção de dados do dispositivo (ex: RPM)
- [X] Testar GUI com dados dinâmicos
- [ ] Teste de desconexão/reconexão Bluetooth

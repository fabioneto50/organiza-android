# Organiza — Android MVP v0.2

Aplicação Android de organização inteligente da vida, com foco inicial em pessoas com horários variáveis e trabalho por turnos.

## O que está funcional nesta versão

- Dashboard "Hoje" com energia, tarefas pendentes, próximo turno e plano do dia.
- Botão **ORGANIZA A MINHA VIDA** para gerar um plano realista para os próximos 7 dias.
- Motor local de planeamento que cruza:
  - turnos;
  - compromissos fixos;
  - tarefas e respetivos prazos;
  - hábitos;
  - prioridade;
  - duração;
  - energia necessária;
  - preferência manhã/tarde/noite;
  - recuperação pós-turno noturno.
- Função **Tenho X minutos**.
- Agenda interna de 14 dias.
- Criação manual de compromissos.
- Importação de eventos existentes no calendário Android (incluindo calendários Google já sincronizados no dispositivo).
- Exportação de compromissos/blocos do plano para o calendário através do ecrã de criação do calendário.
- Gestão de turnos, incluindo importação rápida de padrões de escala.
- Objetivos com percentagem de progresso.
- Hábitos por dias da semana e conclusão diária.
- Reorganização automática do plano quando os dados mudam.
- Lembretes locais através de WorkManager.
- Health Connect opcional: leitura da duração da última sessão de sono para limitar a carga planeada.
- Dados guardados localmente no dispositivo, sem conta obrigatória e sem backend.

## Estrutura do projeto

```text
app/src/main/java/com/organiza/app/
├── data/              Persistência local
├── domain/            Motor de planeamento
├── integrations/      Calendário + Health Connect
├── model/             Modelos de dados
├── notifications/     Lembretes
├── ui/components/     Componentes reutilizáveis
├── ui/screens/        Ecrãs independentes
└── ui/theme/          Tema Compose
```

Isto permite alterar, por exemplo, apenas `PlannerEngine.kt` quando se evolui a inteligência, ou apenas `CalendarScreen.kt` quando se altera a agenda.

## Requisitos técnicos

- Android Studio recente.
- JDK 17 (o Android Studio fornece um JDK integrado).
- Android SDK API 36 para compilar.
- Android 8.0 / API 26 ou superior para instalar a app.
- Gradle 9.5.0 para AGP 9.3.0.
- Internet necessária apenas para o primeiro download das dependências/SDK e para integrações que dependam dos serviços do dispositivo.

### Health Connect

A app continua a funcionar sem Health Connect.

- Android 14 ou superior: Health Connect faz parte do sistema.
- Android 13 ou inferior: poderá ser necessário instalar/atualizar Health Connect.
- A app pede apenas leitura de sono.

## Como abrir e executar

### Opção recomendada — Android Studio

1. Descompactar o ZIP.
2. Abrir o Android Studio.
3. Escolher **Open**.
4. Selecionar a pasta `OrganizaAndroid` — a que contém `settings.gradle.kts`.
5. Se o Android Studio pedir componentes de SDK, aceitar a instalação da API 36/Build Tools necessários.
6. Em **Settings > Build, Execution, Deployment > Build Tools > Gradle**, usar o **Embedded JDK (17)**.
7. Se o IDE pedir explicitamente uma distribuição Gradle, usar **Gradle 9.5.0**.
8. Aguardar o primeiro Gradle Sync terminar.
9. Ligar um Android por USB com **USB debugging** ativo, ou criar um emulador.
10. Selecionar o dispositivo no topo do Android Studio e carregar em **Run ▶**.

> Este pacote não inclui o `gradle-wrapper.jar` binário. Se o teu Android Studio exigir o wrapper em vez de permitir selecionar/descarregar o Gradle, executa `prepare_gradle_wrapper_macos.command` (macOS) ou instala Gradle 9.5.0 e executa `gradle wrapper --gradle-version 9.5.0` na raiz do projeto. Depois volta a abrir/sincronizar o projeto.

## Helper para macOS — gerar o Gradle Wrapper

O ficheiro `prepare_gradle_wrapper_macos.command` descarrega temporariamente Gradle 9.5.0, gera os ficheiros oficiais do wrapper e deixa-os na raiz do projeto.

No Terminal:

```bash
cd /caminho/para/OrganizaAndroid
chmod +x prepare_gradle_wrapper_macos.command
./prepare_gradle_wrapper_macos.command
```

Depois abre o projeto no Android Studio.

## Instalar diretamente num telemóvel durante desenvolvimento

1. No Android: **Definições > Acerca do telefone** e tocar várias vezes em **Número da compilação** até ativar as opções de programador.
2. Em **Opções de programador**, ativar **Depuração USB**.
3. Ligar o telemóvel ao computador por USB.
4. Aceitar no telemóvel a autorização da chave RSA.
5. No Android Studio selecionar esse telemóvel.
6. Carregar em **Run ▶**.

O Android Studio instala a app diretamente no dispositivo.

## Gerar um APK instalável

No Android Studio:

1. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
2. Após terminar, escolher **Locate**.
3. O APK de testes fica normalmente em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Esse APK pode ser enviado para outro dispositivo Android e instalado manualmente, desde que esse dispositivo permita a instalação dessa origem.

Para distribuição pública pela Play Store é necessário gerar uma versão `release` assinada e cumprir as declarações de privacidade/Health Connect aplicáveis.

## Permissões que a app pede

- **Calendário** — apenas quando se escolhe importar eventos.
- **Notificações** — apenas quando se ativam os lembretes em Android 13+.
- **Health Connect / Sono** — apenas quando se ativa a utilização do sono para o planeamento.

A exportação para o calendário abre o ecrã normal de criação de evento, pelo que o utilizador confirma a gravação.

## Códigos rápidos de turnos

Em **Mais > Turnos e escala > Importar padrão**:

- `M` ou `D` — 08:00–16:00
- `T` — 16:00–23:59
- `N` — 20:00–08:00
- `L` ou `12` — 08:00–20:00
- `F` — folga
- `V` — férias

Exemplo:

```text
M M N N F F
```

## Validação efetuada nesta entrega

- O núcleo `Models.kt + PlannerEngine.kt` foi compilado diretamente com Kotlin.
- Foram executados cenários de planeamento com compromisso fixo, recuperação pós-noite e "Tenho X minutos".
- Os XML do projeto e o AndroidManifest foram validados sintaticamente.
- O projeto foi validado no GitHub Actions com JDK 17, Android SDK 36 e Gradle 9.5.0.
- Os testes unitários passaram e o APK debug foi compilado e publicado como artifact do workflow.

## Próximos passos de produto

A arquitetura já permite evoluir para:

- edição/arrastar blocos diretamente no calendário;
- modelos de escalas personalizáveis;
- integração Google Calendar com sincronização bidirecional por conta;
- widgets Android;
- Wear OS;
- backups e sincronização multi-dispositivo;
- motor de IA cloud opcional com linguagem natural;
- aprendizagem progressiva de preferências e duração real das tarefas;
- regras avançadas de descanso, exercício e recuperação.

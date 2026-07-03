# 🎯 Questions & Answers — Quiz Multiplayer em Rede

Jogo de quiz multiplayer desenvolvido em Java com interface gráfica Swing, comunicação cliente-servidor via TCP e mecânica de buzzer competitivo.

Projeto acadêmico — Disciplina de Programação Orientada a Objetos  
IFSP Campus Araraquara

---

## 📋 Requisitos Atendidos

| Requisito | Como foi implementado |
|---|---|
| Mín. 2 jogadores | `ServerSocket` aceita exatamente 2 clientes simultâneos |
| Início e Reinício | Partida inicia automaticamente; reinício via confirmação dos dois jogadores |
| Linguagem Java | 100% Java, sem dependências externas |
| Encapsulamento | Atributos `private` com getters em todas as classes modelo |
| Herança e Abstração | Classe abstrata `EventoJogo` com subclasses concretas |
| Polimorfismo | Servidor chama `evento.aplicar()` sem saber o tipo concreto |
| Threads e Rede | `ServerSocket`, `Socket`, `ObjectStream`, `CountDownLatch`, `AtomicInteger` |
| Leitura de Arquivo | `LeitorPerguntas` lê `perguntas.txt` com `BufferedReader` |
| Escrita de Arquivo | `HistoricoPartidas` grava `historico.txt` com `FileWriter` em modo append |
| Interface Gráfica *(extra)* | Cliente com GUI completa em Java Swing |

---

## 🗂️ Estrutura do Projeto

```
src/
└── quiz/
    ├── modelo/
    │   ├── Mensagem.java         # Protocolo de comunicação (enum Tipo + dado)
    │   ├── Pergunta.java         # Enunciado, alternativas e índice correto
    │   ├── PerguntaRodada.java   # Agrupa Pergunta + número da rodada atual
    │   ├── Partida.java          # Estado da partida: pontos, rodada, nomes
    │   ├── EventoJogo.java       # Classe abstrata base dos eventos de rodada
    │   ├── EventoAcerto.java     # +1 ponto para quem acertou
    │   ├── EventoErro.java       # Jogador erra → perde a vez (sem perda de ponto)
    │   ├── EventoTimeout.java    # Jogador não responde → perde a vez
    │   └── EventoSemResposta.java# Ninguém clicou / segunda chance perdida
    ├── rede/
    │   ├── Servidor.java         # Lógica principal do jogo e controle de rodadas
    │   └── Cliente.java          # Interface gráfica Swing e comunicação com servidor
    └── util/
        ├── LeitorPerguntas.java  # Leitura e parsing do arquivo de perguntas
        └── HistoricoPartidas.java# Escrita e leitura do histórico de partidas

recursos/
├── perguntas.txt                 # Banco de perguntas (formato próprio)
└── historico.txt                 # Gerado automaticamente ao fim de cada partida
```

---

## 🎮 Como Jogar

### Pré-requisitos
- Java 8 ou superior
- NetBeans (recomendado) ou qualquer IDE Java

### Executando

**1. Inicie o Servidor**
```
Execute Servidor.java como classe principal
O terminal exibirá o IP e a porta (12345)
```

**2. Inicie dois Clientes**
```
Execute Cliente.java duas vezes (instâncias separadas)
Digite o IP do servidor e seu nome
Clique em Conectar
```

**3. A partida começa automaticamente quando os dois se conectarem**

---

## 🕹️ Regras do Jogo

```
1.  Uma pergunta é exibida para os dois jogadores simultaneamente
2.  Há 15 segundos para leitura — o buzzer fica bloqueado nesse período
3.  Após os 15s, o buzzer fica vermelho e clicável
4.  Quem clicar primeiro tem 20 segundos para escolher a alternativa
5.  Se acertar       → +1 ponto, próxima pergunta
6.  Se errar         → perde a vez, adversário tem uma segunda chance
7.  Segunda chance   → sem risco de perder ponto se errar
8.  Se não responder → perde a vez, adversário tem segunda chance
9.  O jogo termina quando todas as perguntas forem apresentadas
10. Vence quem tiver mais pontos ao final
```

---

## 📁 Formato do Arquivo de Perguntas

```
PERGUNTA: Qual é a capital do Brasil?
A: São Paulo
B: Rio de Janeiro
C: Brasília
D: Salvador
RESPOSTA: C
---
```

- Linhas iniciadas com `#` são comentários e são ignoradas
- O separador `---` delimita cada pergunta
- As perguntas são embaralhadas a cada partida

---

## 🏗️ Arquitetura

### Comunicação Cliente-Servidor

```
Cliente                          Servidor
   |                                |
   |── NOME ("Igor") ──────────────>|
   |                                |── aguarda 2 nomes
   |<── NOMES_JOGADORES ────────────|
   |<── NOVA_PERGUNTA ──────────────|
   |                                |── aguarda 15s
   |<── LIBERAR_BUZZER ─────────────|
   |── CLICOU_BUZZER ──────────────>|
   |<── VOCE_RESPONDEU ─────────────|
   |── RESPOSTA (2) ───────────────>|
   |<── RESULTADO_RODADA ───────────|
   |         ...                    |
   |<── FIM_DE_JOGO ────────────────|
   |── REINICIAR ──────────────────>|
   |<── AGUARDANDO_REINICIO ────────|
```

### Hierarquia de Eventos (POO)

```
EventoJogo  (abstrata)
├── EventoAcerto       → adicionarPonto() + broadcast RESULTADO_RODADA
├── EventoErro         → broadcast RESULTADO_RODADA (sem alteração de pontos)
├── EventoTimeout      → broadcast RESULTADO_RODADA (sem alteração de pontos)
└── EventoSemResposta  → broadcast RESULTADO_RODADA (sem alteração de pontos)
```

O `Servidor` chama `evento.aplicar()` via polimorfismo, sem conhecer o tipo concreto.
Os callbacks de pontuação e broadcast são injetados via `BiConsumer` e `Consumer`.

---

## 📊 Histórico de Partidas

Ao fim de cada partida, o servidor salva automaticamente em `recursos/historico.txt`:

```
========================================
Data/Hora : 03/07/2026 14:32:10
Rodadas   : 21
Igor      : 5 ponto(s)
Mario     : 3 ponto(s)
Resultado : VENCEDOR: Igor
```

---

## 🔧 Tecnologias e Conceitos

- **Java SE** — linguagem principal
- **Java Swing** — interface gráfica do cliente (`JFrame`, `CardLayout`, `GridBagLayout`)
- **TCP/IP** — comunicação via `ServerSocket` / `Socket`
- **Serialização** — objetos trafegam pela rede via `ObjectOutputStream` / `ObjectInputStream`
- **Concorrência** — `Thread`, `CountDownLatch`, `AtomicInteger`, `volatile`
- **I/O de Arquivo** — `BufferedReader`, `FileWriter`, `BufferedWriter`
- **POO** — Encapsulamento, Herança, Polimorfismo e Abstração

---

## 📝 Entregas

| Entrega | Conteúdo |
|---|---|
| Parcial 1 | Pacote `modelo`, `LeitorPerguntas`, `Servidor` base (aceita conexões) |
| Parcial 2 | `Servidor` completo com buzzer, timer, placar, segunda chance e reinício |
| Parcial 3 | `Cliente` com GUI Swing completa integrada |
| Final | Hierarquia `EventoJogo`, nome customizável, histórico, placar visual, correções |
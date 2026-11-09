# Lê Pra Mim

App Android que lê em voz alta qualquer texto do celular, pensado para quem tem dislexia ou prefere ouvir a ler.

## Como ler um texto

- **Menu do texto:** selecione um texto em qualquer app e toque em **Ler**. Também funciona pelo Compartilhar.
- **Botão Ler no painel rápido:** lê o último texto copiado. Se outro texto for copiado, ele troca e lê o novo. Se for o mesmo, ele para.
- **Ao abrir o app:** se houver um texto novo copiado, a leitura começa sozinha.
- **Marcar na tela:** pelo gesto ou botão de acessibilidade, você marca um trecho da tela e ele é lido, mesmo que o texto não possa ser selecionado. O reconhecimento das letras é feito no próprio aparelho, sem internet.

A leitura continua em segundo plano, com Voltar, Pausar e Parar na notificação.

## Ajustes

- Motor e voz instalados no aparelho (por exemplo, Vocalizer).
- Velocidade da fala.
- Destaque do trecho lido, com o texto rolando sozinho.

## Tecnologia

Kotlin, Jetpack Compose, Material 3, DataStore, TextToSpeech, AccessibilityService e ML Kit Text Recognition. Android 11 ou mais novo.

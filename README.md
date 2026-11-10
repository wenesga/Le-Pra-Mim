# Lê Pra Mim

App Android que lê em voz alta qualquer texto do celular, pensado para quem tem dislexia ou prefere ouvir a ler.

## Como ler um texto

- **Menu do texto:** selecione um texto em qualquer app e toque em **Ler**. Também funciona pelo Compartilhar.
- **Botão Ler na Central de Controle (painel rápido):** lê o último texto copiado. Se outro texto for copiado, ele troca e lê o novo. Se não houver texto novo, ele para a leitura. Para adicionar o botão: puxe o painel, toque em Editar e arraste o botão Ler.
- **Ao abrir o app:** se houver um texto novo copiado, a leitura começa sozinha.
- **Marcar na tela:** pelo gesto ou botão de acessibilidade, você marca um trecho da tela e ele é lido, mesmo que o texto não possa ser selecionado. O reconhecimento das letras é feito no próprio aparelho, sem internet.

Textos copiados de gerenciadores de senhas (marcados como sensíveis pelo Android) não são lidos nem mostrados. Link no fim do texto, como o "Veja mais em" do copia e cola dos sites, não é lido.

## Durante a leitura

- O cartão do app mostra o texto com o trecho lido destacado, a barra de progresso e o tempo (decorrido e total, estimado), com Copiar, Colar e Limpar.
- A leitura continua em segundo plano, num player na notificação com Voltar, Pausar e Avançar, barra de progresso e tempo.

## Ajustes

- Motor e voz instalados no aparelho (por exemplo, Vocalizer).
- Velocidade da fala.
- Destaque do trecho lido, com o texto rolando sozinho.

## Tecnologia

Kotlin, Jetpack Compose, Material 3, DataStore, TextToSpeech, MediaSession, AccessibilityService e ML Kit Text Recognition. Android 11 ou mais novo.

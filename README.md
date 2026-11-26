<div align="center">

<img src="docs/icone.png" width="120" alt="Ícone do Lê Pra Mim">

# Lê Pra Mim

**Um leitor de texto em voz alta para Android** 🔊

Pensado para quem tem dislexia ou simplesmente prefere ouvir a ler.

[![Release](https://img.shields.io/github/v/release/wenesga/Le-Pra-Mim?color=33C48F&label=vers%C3%A3o)](https://github.com/wenesga/Le-Pra-Mim/releases/latest)
![Android](https://img.shields.io/badge/Android-11%2B-33C48F?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-44484F?logo=kotlin&logoColor=white)

</div>

---

## ✨ O que ele faz

Copie, selecione ou aponte para qualquer texto do celular e ouça. Sem precisar abrir nada.

### 📖 Maneiras de ler um texto

| Como | O que acontece |
|------|----------------|
| ✂️ **Menu do texto** | Selecione um texto em qualquer app e toque em **Ler**. Também funciona pelo Compartilhar. |
| ⚡ **Botão Ler na Central de Controle** | Lê o último texto copiado. Texto novo troca e lê; o mesmo texto que está tocando para a leitura. |
| 📋 **Ao abrir o app** | Se houver um texto novo copiado, a leitura começa sozinha. |
| 🖼️ **Marcar na tela** | Pelo gesto ou botão de acessibilidade, marque um trecho da tela e ele é lido, mesmo que o texto não possa ser selecionado. O reconhecimento das letras é feito no próprio aparelho, sem internet. |
| 📂 **Importar** | Abre arquivos **TXT** e **Word (.docx)**. |

> 🔒 Textos copiados de gerenciadores de senhas (marcados como sensíveis pelo Android) não são lidos nem mostrados.

### 🎧 Durante a leitura

- 🟩 Trecho lido **destacado**, com o texto rolando sozinho.
- ✏️ Dá para **editar** o texto, colar, copiar e limpar. O texto fica salvo até você limpar.
- ⏯️ Player completo: **play, pausa, voltar e avançar**, barra que se arrasta e tempo decorrido e total.
- 🚀 **Velocidade** ajustável na própria tela.
- 🔔 A leitura continua em segundo plano, num **player na notificação**.

## 🗣️ Vozes

- 📱 **Neste aparelho:** usa o motor de voz instalado (por exemplo, Vocalizer).
- ☁️ **Azure:** vozes neurais em português do Brasil, com a sua própria chave do Azure Speech. Inclui:
  - ⭐ vozes **favoritas**;
  - 📊 consumo do mês, inclusive o do portal da Azure;
  - 🎵 **Exportar em MP3**.
- 🔁 Se a Azure falhar, a leitura continua com a voz do aparelho.

> 🔑 A chave fica **só no seu aparelho**. Quando a voz da Azure está ligada, o texto lido é enviado ao serviço da Microsoft para virar áudio.

## 🧹 Filtros

Para a leitura ficar limpa, sem mudar o texto da tela:

- 🔗 Links viram só o nome do site.
- 📝 Marcações de Markdown ignoradas.
- 🧩 Regras suas, com expressões regulares.

## 🎨 Aparência

- 🌗 Tema **Sistema, Claro ou Escuro**.
- ⚫ **Sete fundos escuros** à escolha: Preto, Vinho, Grafite, Petróleo, Marinho, Verde e Roxo.
- 🌈 **Seis cores de destaque** e a **Automática**, que acompanha o papel de parede (Android 12+).

## 📥 Instalar

1. Baixe o APK mais recente em **[Releases](https://github.com/wenesga/Le-Pra-Mim/releases/latest)**.
2. Abra o arquivo no celular e permita a instalação de fontes externas, se o Android pedir.
3. Se for usar o **Marcar na tela**, ligue o serviço em *Configurações → Acessibilidade*.

Requer **Android 11 ou mais novo**.

## 🛠️ Tecnologia

Kotlin · Jetpack Compose · Material 3 · DataStore · TextToSpeech · MediaSession · AccessibilityService · ML Kit Text Recognition · Azure Speech

## 🔐 Privacidade

- O reconhecimento de texto na tela (OCR) acontece no aparelho.
- Nada é enviado a servidores, exceto o texto lido quando você escolhe a voz da **Azure**.
- A chave da Azure e as credenciais ficam só no aparelho.

---

<div align="center">

Feito com 💚 por [Wenes](https://github.com/wenesga)

</div>

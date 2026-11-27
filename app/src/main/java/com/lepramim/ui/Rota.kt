package com.lepramim.ui

/** Telas do app. A navegação é uma pilha: voltar tira a última. */
enum class Rota(val titulo: String) {
    LEITOR("Lê Pra Mim"),
    AJUSTES("Ajustes"),
    VOZ("Voz"),
    AZURE("Azure"),
    FILTROS("Filtros"),
    APARENCIA("Aparência"),
    ATALHOS("Atalhos"),
    ESCREVER("Escrever texto"),
    BACKUP("Backup"),
}

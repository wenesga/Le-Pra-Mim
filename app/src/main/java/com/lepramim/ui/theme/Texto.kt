package com.lepramim.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.lepramim.R
import com.lepramim.data.FonteTexto

/** Família de fonte do texto da tela principal. */
fun FonteTexto.familia(): FontFamily = when (this) {
    FonteTexto.PADRAO -> FontFamily.Default
    FonteTexto.ATKINSON -> FontFamily(Font(R.font.atkinson))
    FonteTexto.LEXEND -> FontFamily(Font(R.font.lexend))
    FonteTexto.ANDIKA -> FontFamily(Font(R.font.andika))
    FonteTexto.COMICNEUE -> FontFamily(Font(R.font.comicneue))
    FonteTexto.MULISH -> FontFamily(Font(R.font.mulish))
}

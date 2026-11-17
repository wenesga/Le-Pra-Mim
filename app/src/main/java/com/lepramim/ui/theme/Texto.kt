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
    FonteTexto.OPENDYSLEXIC3 -> FontFamily(Font(R.font.opendyslexic3))
    FonteTexto.OPENDYSLEXIC -> FontFamily(Font(R.font.opendyslexic))
    FonteTexto.OPENDYSLEXICALTA -> FontFamily(Font(R.font.opendyslexicalta))
    FonteTexto.OPENDYSLEXICMONO -> FontFamily(Font(R.font.opendyslexicmono))
    FonteTexto.OPENDYSLEXIC092 -> FontFamily(Font(R.font.opendyslexic092))
    FonteTexto.OPENDYSLEXIC091 -> FontFamily(Font(R.font.opendyslexic091))
    FonteTexto.MULISH -> FontFamily(Font(R.font.mulish))
    FonteTexto.LATO -> FontFamily(Font(R.font.lato))
    FonteTexto.FIRASANS -> FontFamily(Font(R.font.firasans))
    FonteTexto.NOTOSANS -> FontFamily(Font(R.font.notosans))
    FonteTexto.PTSANS -> FontFamily(Font(R.font.ptsans))
}

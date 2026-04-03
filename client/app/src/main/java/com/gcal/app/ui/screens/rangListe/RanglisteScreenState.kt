package com.gcal.app.ui.screens.rangliste

/**
 * RanglisteScreenState - Enum for Screen States
 *
 */
enum class RanglisteScreenState {

    Loading,


    Loaded,


    Error
}

/**
 * RanglisteScreenEvent - Sealed Class for User Actions
 *
 */
sealed class RanglisteScreenEvent {

    data object OnScreenEntered : RanglisteScreenEvent()


    data object OnBackClicked : RanglisteScreenEvent()


    data object OnRetryClicked : RanglisteScreenEvent()
}
package no.carlorrall.fjordvaer.ui

import androidx.lifecycle.viewmodel.CreationExtras
import no.carlorrall.fjordvaer.FjordvaerApplication
import no.carlorrall.fjordvaer.di.AppContainer

/** Lets a ViewModel factory's `initializer { }` block reach the app's container without
 *  every screen having to thread it through by hand. */
val CreationExtras.appContainer: AppContainer
    get() = (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FjordvaerApplication).container

/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProviders
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.extensions.addFragmentToBackstack
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.platform.SimpleFragmentActivity
import im.vector.riotx.core.platform.WaitingViewData
import kotlinx.android.synthetic.main.activity.*
import javax.inject.Inject

class CreateDirectRoomActivity : SimpleFragmentActivity() {

    sealed class Navigation {
        object UsersDirectory : Navigation()
        object Close : Navigation()
        object Previous : Navigation()
    }

    private val viewModel: CreateDirectRoomViewModel by viewModel()
    lateinit var navigationViewModel: CreateDirectRoomNavigationViewModel
    @Inject lateinit var createDirectRoomViewModelFactory: CreateDirectRoomViewModel.Factory
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        super.injectWith(injector)
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar.visibility = View.GONE
        navigationViewModel = ViewModelProviders.of(this, viewModelFactory).get(CreateDirectRoomNavigationViewModel::class.java)
        navigationViewModel.navigateTo.observeEvent(this) { navigation ->
            when (navigation) {
                is Navigation.UsersDirectory -> addFragmentToBackstack(CreateDirectRoomDirectoryUsersFragment(), R.id.container)
                Navigation.Close             -> finish()
                Navigation.Previous          -> onBackPressed()
            }
        }
        if (isFirstCreation()) {
            addFragment(CreateDirectRoomFragment(), R.id.container)
        }
        viewModel.subscribe(this) { renderState(it) }
    }

    private fun renderState(state: CreateDirectRoomViewState) {
        when (state.createAndInviteState) {
            is Loading -> renderCreationLoading()
            is Success -> renderCreationSuccess(state.createAndInviteState())
            is Fail    -> renderCreationFailure(state.createAndInviteState.error)
        }
    }

    private fun renderCreationLoading() {
        updateWaitingView(WaitingViewData(getString(R.string.room_recents_create_room)))
    }

    private fun renderCreationFailure(error: Throwable) {
        hideWaitingView()
        AlertDialog.Builder(this)
                .setMessage(errorFormatter.toHumanReadable(error))
                .setPositiveButton(R.string.ok) { dialog, id -> dialog.cancel() }
                .show()
    }

    private fun renderCreationSuccess(roomId: String?) {
        // Navigate to freshly created room
        if (roomId != null) {
            navigator.openRoom(this, roomId)
        }
        finish()
    }


    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, CreateDirectRoomActivity::class.java)
        }
    }


}
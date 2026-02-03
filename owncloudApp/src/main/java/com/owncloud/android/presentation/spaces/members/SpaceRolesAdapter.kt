/**
 * ownCloud Android client application
 *
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2026 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.presentation.spaces.members

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.RoleItemBinding
import com.owncloud.android.domain.roles.model.OCRole
import com.owncloud.android.domain.roles.model.OCRoleType
import com.owncloud.android.utils.PreferenceUtils

class SpaceRolesAdapter(
    val onRoleSelected: ((OCRole) -> Unit)? = null
): RecyclerView.Adapter<SpaceRolesAdapter.SpaceRolesViewHolder>() {

    private var roles: List<OCRole> = emptyList()
    private var selectedRoleId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceRolesViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val view = inflater.inflate(R.layout.role_item, parent, false)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(parent.context)

        return SpaceRolesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpaceRolesViewHolder, position: Int) {
        val role = roles[position]

        holder.binding.apply {
            roleName.text = role.displayName
            roleDescription.text = role.description
            roleRadioButton.isChecked = role.id == selectedRoleId
            roleRadioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    val previousSelected = selectedRoleId
                    selectedRoleId = role.id
                    previousSelected?.let { previousId -> notifyItemChanged(roles.indexOfFirst { it.id == previousId }) }
                    notifyItemChanged(position)
                    onRoleSelected?.invoke(role)
                }
            }

            roleItemLayout.setOnClickListener {
                roleRadioButton.isChecked = true
            }

            roleIcon.setImageResource(
                when(OCRoleType.parseFromId(role.id)) {
                    OCRoleType.CAN_VIEW -> R.drawable.ic_viewer_role
                    OCRoleType.CAN_EDIT -> R.drawable.ic_lead_pencil_grey
                    OCRoleType.CAN_MANAGE -> R.drawable.ic_share_generic
                    OCRoleType.UNKNOWN_ROLE -> R.drawable.ic_user
                }
            )
        }
    }

    override fun getItemCount(): Int = roles.size

    fun setRoles(roles: List<OCRole>) {
        this.roles = roles
        notifyDataSetChanged()
    }

    fun setSelectedRole(id: String) {
        this.selectedRoleId = id
        notifyDataSetChanged()
    }

    class SpaceRolesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = RoleItemBinding.bind(itemView)
    }
}

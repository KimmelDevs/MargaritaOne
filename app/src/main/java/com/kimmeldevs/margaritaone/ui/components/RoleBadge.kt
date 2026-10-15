package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.model.UserRole
import com.kimmeldevs.margaritaone.i18n.t

/** Small "Admin" (shield) or "Staff" (ID badge) pill next to a name. Residents get none. */
@Composable
fun RoleBadge(role: UserRole?, modifier: Modifier = Modifier) {
    val isAdmin = role == UserRole.ADMIN
    if (role == null || !role.isStaff) return
    val tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.13f),
        contentColor = tint,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Icon(
                if (isAdmin) Icons.Filled.AdminPanelSettings else Icons.Filled.Badge,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(t(role.label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "Posted by Juan Dela Cruz [Admin]": a line of text followed by the person's role badge. */
@Composable
fun AuthorLine(text: String, role: UserRole?, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Text(
            text,
            style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoleBadge(role)
    }
}

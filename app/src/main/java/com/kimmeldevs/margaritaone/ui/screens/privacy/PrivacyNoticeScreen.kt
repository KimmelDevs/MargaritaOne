package com.kimmeldevs.margaritaone.ui.screens.privacy

import com.kimmeldevs.margaritaone.i18n.t
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// TODO(LGU): once the LGU designates a Data Protection Officer, put their name, email and phone here and have them
// review this notice. No DPO is publicly listed for Santa Margarita (checked Oct 2026), so requests go to the Mayor's office.
private const val DPO_CONTACT = "Data Protection Officer, Office of the Municipal Mayor, Municipal Hall, Santa Margarita, Samar 6709"

private val sections = listOf(
    "Who we are" to
        "MargaritaOne is operated by the Local Government Unit of Santa Margarita, Samar. We process your personal " +
        "information in line with the Data Privacy Act of 2012 (Republic Act No. 10173).",
    "What we collect" to
        "• Account details: your name, email address, and (if you add them) phone number and barangay.\n" +
        "• Reports you submit: title, description, category, photo, and the GPS location you attach.\n" +
        "• Technical data needed to keep you signed in.",
    "Why we collect it" to
        "To receive, act on, and update you about the problems you report, to contact you if we need more " +
        "details, and to publish public announcements. We do not use your data for advertising and we do not sell it.",
    "Who can see it" to
        "Your reports, photos, and location are visible only to you and to authorized LGU staff. Staff can also " +
        "see your name, email, phone, and barangay so they can follow up. Bulletin posts are public. " +
        "Data is stored with our hosting provider, Supabase, under access rules that enforce these limits.",
    "How long we keep it" to
        "Reports are kept for as long as needed to resolve them and for the LGU's record-keeping obligations. " +
        "You may ask us to delete your account and personal data at any time.",
    "Your rights" to
        "You have the right to be informed, to access, to correct, to object, to erasure or blocking, to data " +
        "portability, and to file a complaint with the National Privacy Commission (privacy.gov.ph).",
    "Contact" to
        "For privacy questions or requests, contact: %s"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyNoticeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(t("Privacy notice")) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            sections.forEach { (title, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        t(body, DPO_CONTACT),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

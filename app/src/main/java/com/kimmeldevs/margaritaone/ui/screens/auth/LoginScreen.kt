package com.kimmeldevs.margaritaone.ui.screens.auth

import com.kimmeldevs.margaritaone.i18n.t
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.ui.components.BrandBanner
import com.kimmeldevs.margaritaone.ui.components.LanguagePicker
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.ui.viewmodel.AuthFormState

@Composable
fun LoginScreen(
    state: AuthFormState,
    onSignIn: (email: String, password: String) -> Unit,
    onGoToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenHotlines: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            LanguagePicker(modifier = Modifier.align(Alignment.CenterHorizontally))
            BrandBanner()
            Text(
                text = t("MargaritaOne"),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = t("Report problems and get updates from the Municipality of Santa Margarita."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(t("Email")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(t("Password")) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { PasswordVisibilityToggle(showPassword) { showPassword = !showPassword } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            FormMessages(state)

            Button(
                onClick = { onSignIn(email, password) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(t("Sign in"))
                }
            }
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(t("Forgot password?"))
            }
            TextButton(onClick = onGoToSignUp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(t("No account yet? Sign up"))
            }
            OutlinedButton(
                onClick = onOpenHotlines,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(t("Emergency hotlines"), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
internal fun FormMessages(state: AuthFormState) {
    state.error?.let {
        Text(text = t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    state.info?.let {
        Text(text = t(it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }
}

package com.kimmeldevs.margaritaone.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.BrandWaves
import com.kimmeldevs.margaritaone.ui.components.GradientButton
import com.kimmeldevs.margaritaone.ui.components.LanguagePicker
import com.kimmeldevs.margaritaone.ui.components.MargaritaOneLogo
import com.kimmeldevs.margaritaone.ui.theme.BackgroundGradient
import com.kimmeldevs.margaritaone.ui.viewmodel.AuthFormState

@Composable
fun LoginScreen(
    state: AuthFormState,
    onSignIn: (email: String, password: String) -> Unit,
    onGoToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onOpenHotlines: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAbout: () -> Unit = {}
) {
    var email by rememberSaveable { mutableStateOf("") }
    // Coming back from sign-up: fill in the email that was just used.
    LaunchedEffect(state.prefillEmail) {
        state.prefillEmail?.let { email = it }
    }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            LanguagePicker()
            MargaritaOneLogo(stacked = true, logoHeight = 64.dp, modifier = Modifier.padding(top = 12.dp))
            Text(
                t("Report · Request · Connect"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(t("Sta. Margarita, Samar"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Town illustration with the mascot, faded into the background at the top.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(522f / 214f)
        ) {
            Image(
                painter = painterResource(R.drawable.login_hero),
                contentDescription = t("Santa Margarita church and the crab mascot"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color.Transparent)))
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(t("Welcome Back!"), style = MaterialTheme.typography.headlineMedium)
            Text(t("Sign in to your account"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text(t("Email")) },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text(t("Password")) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { PasswordVisibilityToggle(showPassword) { showPassword = !showPassword } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            FormMessages(state)

            GradientButton(
                text = t("Sign In"),
                onClick = { onSignIn(email, password) },
                loading = state.isLoading,
                showArrow = true,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(t("Forgot password?"), color = MaterialTheme.colorScheme.secondary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t("No account yet?"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onGoToSignUp) {
                    Text(t("Sign up"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = onOpenHotlines,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(t("Emergency hotlines"), color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(start = 8.dp))
            }
            TextButton(onClick = onOpenAbout, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(t("About Santa Margarita"))
            }
        }
        BrandWaves(modifier = Modifier.padding(top = 8.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .navigationBarsPadding()
        )
    }
}

@Composable
internal fun FormMessages(state: AuthFormState) {
    state.error?.let {
        Text(text = t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    state.info?.let {
        Text(text = t(it), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
    }
}

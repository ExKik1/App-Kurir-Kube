package com.example.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeBlueDark
import com.example.ui.theme.KubeSkyGradient
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    initialIsSignUp: Boolean = false,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(initialIsSignUp) }
    var email by remember { mutableStateOf("maryonosupardi@kube.com") }
    var password by remember { mutableStateOf("password123") }
    var confirmPassword by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var agreeToTerms by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val handleAuthSubmit: () -> Unit = {
        if (!isLoading) {
            coroutineScope.launch {
                try {
                    if (isSignUp && password != confirmPassword) {
                        snackbarHostState.showSnackbar("Konfirmasi kata sandi tidak cocok.")
                        return@launch
                    }
                    if (isSignUp && !agreeToTerms) {
                        snackbarHostState.showSnackbar("Harap setujui Syarat & Ketentuan terlebih dahulu.")
                        return@launch
                    }
                    isLoading = true
                    val result = CourierSupabaseService.instance.loginCourier(
                        email = email,
                        password = password
                    )
                    isLoading = false
                    result.fold(
                        onSuccess = {
                            onAuthSuccess()
                        },
                        onFailure = { error ->
                            snackbarHostState.showSnackbar(
                                error.message ?: "Gagal masuk ke akun kurir. Periksa kredensial Anda."
                            )
                        }
                    )
                } catch (e: Exception) {
                    isLoading = false
                    snackbarHostState.showSnackbar("Terjadi kendala saat autentikasi: ${e.message ?: "Coba lagi."}")
                }
            }
        }
    }

    // Explicit high-contrast text color (Colors.black87)
    val inputBlack87 = Color(0xDE000000)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KubeSkyGradient)
            .testTag("auth_screen")
    ) {
        val viewportHeight = maxHeight
        var headerHeightDp by remember { mutableStateOf(0.dp) }
        val minFormHeight = (viewportHeight - headerHeightDp).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header Area (Status bar padding + Back button + Single Center Logo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        headerHeightDp = with(density) { size.height.toDp() }
                    }
                    .statusBarsPadding()
            ) {
                // Top Navigation (Back Button only, clean minimal top bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Circular Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                            .align(Alignment.CenterStart)
                            .testTag("auth_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Single Main Center Logo: Official Kube Ice Cube with Soft Glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Soft glowing ambient circular background
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00B4DB).copy(alpha = 0.22f))
                        )
                        Image(
                            painter = painterResource(id = R.drawable.logo_kube_cube),
                            contentDescription = "Official Logo Kube Cube",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(92.dp)
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // White Form Container: stretches all the way to the very bottom edge (zero blue gap)
            // Rounded top corners ONLY (32.dp), flat bottom corners (0.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minFormHeight),
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    topEnd = 32.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                color = Color.White,
                shadowElevation = 16.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)
                ) {
                // Segmented Tab Toggle [ Log In | Sign Up ]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFE8F4F8))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val loginBgModifier = if (!isSignUp) {
                            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF70C9E8), Color(0xFF42A5F5))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }

                        val signUpBgModifier = if (isSignUp) {
                            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF70C9E8), Color(0xFF42A5F5))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }

                        // Log In Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .then(loginBgModifier)
                                .clickable { isSignUp = false }
                                .testTag("tab_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log In",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUp) Color.White else Color(0xFF0288D1)
                            )
                        }

                        // Sign Up Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .then(signUpBgModifier)
                                .clickable { isSignUp = true }
                                .testTag("tab_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUp) Color.White else Color(0xFF0288D1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Email Field (Guaranteed black87 text color for high contrast)
                Text(
                    text = "Email",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KubeTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = TextStyle(
                        color = inputBlack87,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    placeholder = { Text("nama@gmail.com", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = "Email", tint = Color(0xFF64748B))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email"),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inputBlack87,
                        unfocusedTextColor = inputBlack87,
                        focusedBorderColor = KubeBlueBright,
                        unfocusedBorderColor = Color(0xFFCFD8DC),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF94A3B8),
                        cursorColor = KubeBlueBright
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field (Guaranteed black87 text color for high contrast)
                Text(
                    text = "Password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KubeTextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    textStyle = TextStyle(
                        color = inputBlack87,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = "Password", tint = Color(0xFF64748B))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password"),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inputBlack87,
                        unfocusedTextColor = inputBlack87,
                        focusedBorderColor = KubeBlueBright,
                        unfocusedBorderColor = Color(0xFFCFD8DC),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF94A3B8),
                        cursorColor = KubeBlueBright
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                    )
                )

                // Confirm Password (Sign Up only)
                AnimatedVisibility(visible = isSignUp) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Confirm Password",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KubeTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            textStyle = TextStyle(
                                color = inputBlack87,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            placeholder = { Text("••••••••", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = "Confirm", tint = Color(0xFF64748B))
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = "Toggle confirm password",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_confirm_password"),
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = inputBlack87,
                                unfocusedTextColor = inputBlack87,
                                focusedBorderColor = KubeBlueBright,
                                unfocusedBorderColor = Color(0xFFCFD8DC),
                                focusedPlaceholderColor = Color(0xFF94A3B8),
                                unfocusedPlaceholderColor = Color(0xFF94A3B8),
                                cursorColor = KubeBlueBright
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Checkbox & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = if (isSignUp) agreeToTerms else rememberMe,
                            onCheckedChange = {
                                if (isSignUp) agreeToTerms = it else rememberMe = it
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = KubeBlueBright
                            )
                        )
                        Text(
                            text = if (isSignUp) "Agree To Terms & Privacy" else "Remember me",
                            fontSize = 12.sp,
                            color = KubeTextDark
                        )
                    }

                    if (!isSignUp) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KubeTextMuted,
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Kata sandi dikelola Admin Kube. Hubungi Dispatcher untuk reset akses.")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Button (Gradient Cyan-Blue)
                Button(
                    onClick = handleAuthSubmit,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.horizontalGradient(listOf(KubeBlueBright, KubeBlueDark))
                        )
                        .testTag("auth_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.4.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Sign Up" else "Log In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Social Login divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                    Text(
                        text = "  or continue with  ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google & Apple Buttons (OutlinedButton with grey.shade300 border and circular 30dp radius)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Google Button
                    OutlinedButton(
                        onClick = onAuthSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("google_auth_button"),
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = KubeTextDark
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeTextDark
                            )
                        }
                    }

                    // Apple Button
                    OutlinedButton(
                        onClick = onAuthSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("apple_auth_button"),
                        shape = RoundedCornerShape(30.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = KubeTextDark
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_apple_logo),
                                contentDescription = "Apple Logo",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apple",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeTextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom switch prompt with safe bottom spacing above system navigation bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                        fontSize = 13.sp,
                        color = KubeTextDark
                    )
                    Text(
                        text = if (isSignUp) "Log in" else "Sign Up",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeBlueDark,
                        modifier = Modifier.clickable { isSignUp = !isSignUp }
                    )
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}


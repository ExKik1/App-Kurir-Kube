package com.example.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeBlueDark
import com.example.ui.theme.KubeSkyGradient
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

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

    val scrollState = rememberScrollState()

    // Explicit high-contrast text color (Colors.black87)
    val inputBlack87 = Color(0xDE000000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KubeSkyGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top Navigation & Official Logo Branding
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

                // Header Official Logo
                Image(
                    painter = painterResource(id = R.drawable.logo_kube),
                    contentDescription = "Official Kube Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(42.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Center Logo / Mascot Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSignUp) 130.dp else 115.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSignUp) {
                    Image(
                        painter = painterResource(id = R.drawable.kube_mascot_cooler),
                        contentDescription = "Mascot Kube Cooler",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(125.dp)
                    )
                } else {
                    // Official Logo Kube with BoxFit.contain
                    Image(
                        painter = painterResource(id = R.drawable.logo_kube),
                        contentDescription = "Official Logo Kube",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // White Form Container with Rounded Top Corners
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
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
                            modifier = Modifier.clickable { }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action Button (Gradient Cyan-Blue)
                Button(
                    onClick = onAuthSuccess,
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
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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

                // Google & Apple Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(24.dp))
                            .clickable(onClick = onAuthSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "G", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEA4335))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Google", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KubeTextDark)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(24.dp))
                            .clickable(onClick = onAuthSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Apple", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KubeTextDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom switch prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
}

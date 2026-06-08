package com.mapchina.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapchina.ui.theme.MapChinaColors

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onQuickStart: (String) -> Unit = {},
    onPhoneLogin: (phone: String, code: String) -> Unit = { _, _ -> },
    onSendCode: (phone: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var isPhoneMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MapChinaColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Gradient icon background
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MapChinaColors.Primary,
                            MapChinaColors.PrimaryVariant
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Explore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "MapChina",
            color = MapChinaColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "用地图点亮你的中国足迹",
            color = MapChinaColors.TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isPhoneMode) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("输入昵称快速开始", color = MapChinaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MapChinaColors.TextPrimary,
                    unfocusedTextColor = MapChinaColors.TextSecondary,
                    focusedBorderColor = MapChinaColors.Primary,
                    unfocusedBorderColor = MapChinaColors.BorderSubtle,
                    cursorColor = MapChinaColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (nickname.isNotBlank()) {
                        onQuickStart(nickname.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nickname.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
            ) {
                Text("快速开始", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { isPhoneMode = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MapChinaColors.Primary)
            ) {
                Text("手机号登录")
            }
        } else {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号", color = MapChinaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MapChinaColors.TextPrimary,
                    unfocusedTextColor = MapChinaColors.TextSecondary,
                    focusedBorderColor = MapChinaColors.Primary,
                    unfocusedBorderColor = MapChinaColors.BorderSubtle,
                    cursorColor = MapChinaColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (codeSent) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("验证码", color = MapChinaColors.TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MapChinaColors.TextPrimary,
                        unfocusedTextColor = MapChinaColors.TextSecondary,
                        focusedBorderColor = MapChinaColors.Primary,
                        unfocusedBorderColor = MapChinaColors.BorderSubtle,
                        cursorColor = MapChinaColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPhoneLogin(phone, code) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length >= 4 && phone.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) {
                    Text("登录", fontWeight = FontWeight.Medium)
                }
            } else {
                Button(
                    onClick = { onSendCode(phone); codeSent = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phone.length >= 11,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) {
                    Text("获取验证码", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    isPhoneMode = false
                    codeSent = false
                    code = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MapChinaColors.TextTertiary)
            ) {
                Text("返回昵称登录")
            }
        }
    }
}

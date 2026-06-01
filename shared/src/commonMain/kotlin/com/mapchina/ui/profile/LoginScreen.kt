package com.mapchina.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
            .background(Color(0xFF0F1923))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(MapChinaColors.Primary.copy(alpha = 0.2f)),
            tint = MapChinaColors.Primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "MapChina",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "用地图点亮你的中国足迹",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isPhoneMode) {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("输入昵称快速开始", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MapChinaColors.Primary,
                    unfocusedBorderColor = Color(0xFF213647),
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
                colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
            ) {
                Text("快速开始")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { isPhoneMode = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MapChinaColors.Primary)
            ) {
                Text("手机号登录")
            }
        } else {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MapChinaColors.Primary,
                    unfocusedBorderColor = Color(0xFF213647),
                    cursorColor = MapChinaColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (codeSent) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("验证码", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MapChinaColors.Primary,
                        unfocusedBorderColor = Color(0xFF213647),
                        cursorColor = MapChinaColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLoginSuccess,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.length >= 4 && phone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) {
                    Text("登录")
                }
            } else {
                Button(
                    onClick = { codeSent = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phone.length >= 11,
                    colors = ButtonDefaults.buttonColors(containerColor = MapChinaColors.Primary)
                ) {
                    Text("获取验证码")
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
            ) {
                Text("返回昵称登录")
            }
        }
    }
}

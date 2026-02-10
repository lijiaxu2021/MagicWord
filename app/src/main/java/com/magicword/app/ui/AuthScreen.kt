package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.magicword.app.network.AuthRequest
import com.magicword.app.network.SyncClient
import com.magicword.app.utils.AuthManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "登录 MagicWord" else "注册账号",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        val request = AuthRequest(username, password)
                        val response = if (isLogin) {
                            SyncClient.api.login(request)
                        } else {
                            SyncClient.api.register(request)
                        }
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            // Assuming user_id is returned on login too, if not we might need adjustment
                            // The server code I wrote returns user_id for register and login.
                            AuthManager.saveUser(context, body.user_id ?: 0, username)
                            onLoginSuccess()
                        } else {
                            // Try to parse JSON error if possible, or just raw string
                            error = "验证失败，请检查用户名或密码"
                        }
                    } catch (e: Exception) {
                        error = "网络错误: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (isLogin) "登录" else "注册")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = { isLogin = !isLogin; error = null }) {
            Text(if (isLogin) "没有账号？去注册" else "已有账号？去登录")
        }
    }
}

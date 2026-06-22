package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val strings = t()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.welcomeToApp,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LovePink,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(strings.start, fontSize = 18.sp)
        }
    }
}

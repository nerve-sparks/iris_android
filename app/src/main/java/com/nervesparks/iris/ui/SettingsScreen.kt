package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.R

@Composable
fun SettingsScreen(
    onParamsScreenButtonClicked: () -> Unit,
    onModelsScreenButtonClicked: () -> Unit,
    onAboutScreenButtonClicked: () -> Unit,
    onBenchMarkScreenButtonClicked: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                ) {
                    SettingsRow(
                        text = "Models",
                        iconRes = R.drawable.data_exploration_models_svgrepo_com,
                        onClick = onModelsScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "Change Parameters",
                        iconRes = R.drawable.setting_4_svgrepo_com,
                        onClick = onParamsScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "BenchMark",
                        iconRes = R.drawable.bench_mark_icon,
                        onClick = onBenchMarkScreenButtonClicked
                    )

                    SettingsDivider()

                    SettingsRow(
                        text = "About",
                        iconRes = R.drawable.information_outline_svgrepo_com,
                        onClick = onAboutScreenButtonClicked
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsRow(text: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 7.dp)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun SettingsDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.DarkGray,
        thickness = 1.dp
    )
}

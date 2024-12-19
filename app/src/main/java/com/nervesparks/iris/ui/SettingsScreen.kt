package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.LinearGradient
import com.nervesparks.iris.R

@Composable
fun SettingsScreen(
    OnParamsScreenButtonClicked:    () -> Unit,
    OnModelsScreenButtonClicked:    () -> Unit,
    OnAboutScreenButtonClicked:     () -> Unit,
    OnBenchMarkScreenButtonClicked: () -> Unit,
    OnBackButtonClicked:         (Int) -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

            LazyColumn (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                item {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    )
                    {
                        Row(

                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    OnModelsScreenButtonClicked()
                                }

                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.data_exploration_models_svgrepo_com),
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Models",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 7.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp), // Optional: Adjust horizontal padding if needed
                            color = Color.DarkGray, // Set the color of the divider
                            thickness = 1.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    OnParamsScreenButtonClicked()
                                }
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp), // Icon size,
                                painter = painterResource(id = R.drawable.setting_4_svgrepo_com),
                                contentDescription = "Parameters",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Change Parameters",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 7.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp), // Optional: Adjust horizontal padding if needed
                            color = Color.DarkGray, // Set the color of the divider
                            thickness = 1.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    OnBenchMarkScreenButtonClicked()
                                }
                        ) {
                            Icon(
                                modifier = Modifier.size(21.dp), // Icon size,
                                painter = painterResource(id = R.drawable.bench_mark_icon),
                                contentDescription = "Parameters",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "BenchMark",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 7.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp), // Optional: Adjust horizontal padding if needed
                            color = Color.DarkGray, // Set the color of the divider
                            thickness = 1.dp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    OnAboutScreenButtonClicked()
                                }
                        ) {
                            Icon(
                                modifier = Modifier.size(21.dp), // Icon size,
                                painter = painterResource(id = R.drawable.information_outline_svgrepo_com),
                                contentDescription = "Parameters",
                                tint = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "About",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 7.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
    }


}

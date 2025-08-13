package com.nervesparks.iris.ui

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.ModelCard
import com.nervesparks.iris.ui.theme.ComponentStyles
import java.io.File

@Composable
fun ModelsScreen(extFileDir: File?, viewModel: MainViewModel, onSearchResultButtonClick: () -> Unit, dm: DownloadManager, onQuantizeScreenButtonClicked: () -> Unit) {
    // Observe viewModel.refresh to trigger recomposition
    val refresh = viewModel.refresh

    // Reset refresh to false after the screen is recomposed
    if (refresh) {
        viewModel.refresh = false
    }

    val context = LocalContext.current
    val preferencesRepository = UserPreferencesRepository.getInstance(context)

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        if (viewModel.showAlert) {
            // Modal dialog to show download options
            LoadingModal(
                message = "Loading Model",
                onDismiss = { }
            )
        }

        val suggestedModels = viewModel.allModels.filter { it["supportsReasoning"] == "true" }.take(3)

        LazyColumn(modifier = Modifier.padding(horizontal = ComponentStyles.defaultPadding)) {
            item {
                Column {
                    if (!preferencesRepository.hasHuggingFaceCredentials()) {
                        Text(
                            text = "Tip: Add your HuggingFace token in Settings to enable online search and downloads",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ComponentStyles.defaultSpacing)
                        )
                        Spacer(Modifier.height(ComponentStyles.smallPadding))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ComponentStyles.defaultSpacing, vertical = ComponentStyles.smallPadding)
                            .clickable {
                                onSearchResultButtonClick()
                            }
                    ) {
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.search_svgrepo_com__3_),
                            contentDescription = "Parameters",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(ComponentStyles.defaultSpacing))
                        Text(
                            text = "Search Hugging-Face Models",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(vertical = ComponentStyles.defaultSpacing, horizontal = ComponentStyles.smallPadding)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ComponentStyles.defaultSpacing, vertical = ComponentStyles.smallPadding)
                            .clickable {
                                onQuantizeScreenButtonClicked()
                            }
                    ) {
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.ic_quantize),
                            contentDescription = "Quantize",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(ComponentStyles.defaultSpacing))
                        Text(
                            text = "Quantize Models",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .padding(vertical = ComponentStyles.defaultSpacing, horizontal = ComponentStyles.smallPadding)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline,
                        thickness = ComponentStyles.defaultBorderWidth
                    )
                    Spacer(Modifier.height(ComponentStyles.largePadding))
                    // Suggested Models Section
                    Text(
                        text = "Suggested Models",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(ComponentStyles.smallPadding),
                        fontSize = 18.sp
                    )
                }
            }

            // Show first three suggested models that support reasoning
            items(items = suggestedModels, key = { it["name"] ?: it.hashCode().toString() }) { model ->
                extFileDir?.let {
                    model["source"]?.let { source ->
                        ModelCard(
                            model["name"].toString(),
                            supportsReasoning = model["supportsReasoning"] == "true",
                            supportsVision = model["supportsVision"] == "true",
                            viewModel = viewModel,
                            dm = dm,
                            extFilesDir = extFileDir,
                            downloadLink = source,
                            showDeleteButton = true
                        )
                    }
                }
            }
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = ComponentStyles.defaultBorderWidth
                )
            }

            item {
                // My Models Section
                Text(
                    text = "My Models",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ComponentStyles.smallPadding),
                    fontSize = 18.sp
                )
            }

            // Display all models not in Suggested Models
            items(items = viewModel.allModels.filterNot { suggestedModels.contains(it) }, key = { it["name"] ?: it.hashCode().toString() }) { model ->
                extFileDir?.let {
                    model["source"]?.let { source ->
                        ModelCard(
                            model["name"].toString(),
                            supportsReasoning = model["supportsReasoning"] == "true",
                            supportsVision = model["supportsVision"] == "true",
                            viewModel = viewModel,
                            dm = dm,
                            extFilesDir = extFileDir,
                            downloadLink = source,
                            showDeleteButton = true
                        )
                    }
                }
            }
            item {
                if (viewModel.allModels.drop(3).isEmpty()) {
                    Text(
                        text = "No models to show",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = ComponentStyles.smallPadding, start = ComponentStyles.smallPadding)
                    )
                }
            }
        }
    }
}
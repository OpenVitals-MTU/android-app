package tech.mmarca.openvitals

import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsButton

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenVitalsTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OpenVitalsCard(
                        modifier = Modifier.fillMaxWidth(),

                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.privacy_policy_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_body_1),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_body_2),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Text(
                                text = stringResource(R.string.privacy_policy_body_3),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            OpenVitalsButton(
                                onClick = ::finish,
                                modifier = Modifier.padding(top = 20.dp),
                            ) {
                                Text(stringResource(R.string.action_close))
                            }
                        }
                    }
                }
            }
        }
    }
}

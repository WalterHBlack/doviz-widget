package com.walterhblack.dovizwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walterhblack.dovizwidget.ui.ConverterScreen
import com.walterhblack.dovizwidget.ui.ConverterViewModel

// Android uygulamayı açınca buraya gelir. Ekranın ayrıntıları ui klasöründedir.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: ConverterViewModel = viewModel()
            ConverterScreen(model)
        }
    }
}

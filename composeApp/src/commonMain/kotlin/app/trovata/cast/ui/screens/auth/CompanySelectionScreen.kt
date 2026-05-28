package app.trovata.cast.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.trovata.cast.data.auth.Company
import app.trovata.cast.theme.TrovataTokens
import app.trovata.cast.ui.icons.TrovataIcons

@Composable
fun CompanySelectionScreen(
    companies: List<Company>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSelect: (Company) -> Unit = {},
) {
    val colors = TrovataTokens.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            AuthTopBar(onBack = onBack)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text(
                    text = "EMPRESA",
                    color = colors.brand2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.em,
                )
                Text(
                    text = "Escolha a empresa",
                    color = colors.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).em,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Você tem acesso a ${companies.size} empresas. A escolhida agora será a ativa.",
                    color = colors.ink3,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(companies, key = { it.id }) { company ->
                    CompanyRow(company = company, onClick = { onSelect(company) })
                }
            }
        }
    }
}

@Composable
private fun CompanyRow(company: Company, onClick: () -> Unit) {
    val colors = TrovataTokens.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(colors.brandTint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = company.name.take(1).uppercase(),
                color = colors.brand,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = company.name,
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val subtitle = company.legalName ?: company.cnpj
            if (subtitle != null) {
                Text(text = subtitle, color = colors.ink4, fontSize = 12.sp, maxLines = 1)
            }
        }
        Icon(
            imageVector = TrovataIcons.arrowRight,
            contentDescription = null,
            tint = colors.ink4,
            modifier = Modifier.size(18.dp),
        )
    }
}

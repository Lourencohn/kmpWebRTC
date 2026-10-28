package app.trovata.cast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trovata.cast.ui.icons.TrovataIcons
import app.trovata.cast.ui.theme.TrovataTokens

enum class SellerTab(val id: String, val label: String, val icon: ImageVector) {
    Sessions("sessoes", "Início", TrovataIcons.home),
    Agenda("agenda", "Agenda", TrovataIcons.calendar),
    Carts("carrinhos", "Carrinhos", TrovataIcons.cart),
    Catalogs("catalogos", "Catálogos", TrovataIcons.grid),
    Recordings("gravacoes", "Gravações", TrovataIcons.record),
    Clients("clientes", "Clientes", TrovataIcons.users),
}

private val primaryTabs = listOf(SellerTab.Sessions, SellerTab.Agenda, SellerTab.Catalogs, SellerTab.Carts)
private val moreTabs = listOf(SellerTab.Clients, SellerTab.Recordings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabBar(
    active: SellerTab,
    onSelect: (SellerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    var showMore by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(color = colors.line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .selectableGroup()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            primaryTabs.forEach { tab ->
                TabDestination(
                    label = tab.label,
                    icon = tab.icon,
                    selected = tab == active,
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
            TabDestination(
                label = "Mais",
                icon = TrovataIcons.more,
                selected = active in moreTabs,
                onClick = { showMore = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showMore) {
        ModalBottomSheet(
            onDismissRequest = { showMore = false },
            containerColor = colors.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Mais opções", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                moreTabs.forEach { tab ->
                    val selected = tab == active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) colors.brandTint else colors.surface2)
                            .selectable(selected = selected, role = Role.Tab) {
                                showMore = false
                                onSelect(tab)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = colors.brand, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tab.label, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (tab == SellerTab.Clients) "Encontrar, convidar e agendar" else "Apresentações para compartilhar",
                                color = colors.ink3,
                                fontSize = 13.sp,
                            )
                        }
                        Icon(
                            if (selected) TrovataIcons.check else TrovataIcons.chev,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabDestination(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TrovataTokens.colors
    val foreground = if (selected) colors.brand else colors.ink3
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .background(if (selected) colors.brandTint else Color.Transparent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(23.dp))
        }
        Text(
            text = label,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

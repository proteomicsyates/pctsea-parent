library(shiny)
library(dplyr)
library(stringr)
library(ggplot2)
library(sjmisc)
library(tidyverse)
library(data.table)
library(promises)
library(future)
library(tools)
library(stringi)
library(filesstrings)
library(DT)

setwd("C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/Spike_DandG_results/2020-12-16_11-01-09_Spike_DandG_results/cell_types_charts")
cell_type <- "adrenocortical"
table = fread(file = paste0("Spike_DandG_TYPE_",cell_type,"_ews.txt"), header = FALSE, sep = "\t")

names(table) <- c('cell type', 'rank', 'cumulative probability')
type <- table[table$`cell type` == cell_type, ]
other <- table[table$`cell type` == 'others', ]

ggplot(data = table,
       aes(
         x = rank,
         y = `cumulative probability`,
         group = `cell type`)) +
  labs(title = paste0("Enrichment score calculation for cell type: '",cell_type, "'"), x = "cell #", y = "Cumulative Probability") +
  geom_line(aes(color = `cell type`))+
  theme_classic() +
  xlim(1,max(table$rank)) +
  ylim(0,1)



table = fread(file = paste0("Spike_DandG_TYPE_",cell_type,"_corr.txt"), header = FALSE, sep = "\t")

names(table) <- c('cell type', 'Pearson\'s correlation', 'Frequency (# cells)')
ggplot(data = table,
       aes(
         x = `Pearson\'s correlation`,
         y = `Frequency (# cells)`)) +
  labs(title = paste0("Pearson's correlation distribution for cell type: '",cell_type, "'"), x = "Pearson\'s correlation", y = "Frequency (# cells)") +
  geom_line(aes(color = `cell type`))+
  theme_classic() +
  xlim(-1,1)
  # ylim(0,1)



table = fread(file = paste0("Spike_DandG_TYPE_",cell_type,"_genes_per_cell_hist.txt"), header = FALSE, sep = "\t")



ggplot(data = table, xlab='# of genes correlating') +
  geom_bar(stat="identity", aes(x=factor(V2), y=V3, color = V1, fill = V1), position = 'dodge') +
  labs(title = paste0("Distribution of # of genes correlating in cell type: '",cell_type, "'"), x = "# of genes correlating", y = "# cells") +
  theme_classic()



setwd("C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/Spike_DandG_results/2020-12-16_11-01-09_Spike_DandG_results/global_charts")
table = fread(file = paste0("Spike_DandG_corr_rank_dist.txt"), header = FALSE, sep = "\t")
names(table) <- c('x', 'Rank', 'Pearson\'s correlation')
plot <- ggplot(data = table,
               aes(
                 x = Rank,
                 y = `Pearson\'s correlation`)) +
  labs(x = "ranked cells", y = "Pearson\'s correlation") +
  geom_line(aes(color = x))+
  theme_classic() +
  theme(legend.title = element_blank())
# ggtitle(paste0("Corr. distrib. for: '",cell_type, "'")) +
# theme(plot.title = element_text(size=10))
ggplotly(plot) %>%
  layout(
    legend = list(
      orientation = "v",
      x = 0.5,
      y = 0.6,
      title = list(text = "cell type", side = "top")
    )
  )




table = fread(file = paste0("Spike_DandG_TYPE_suprema_scatter.txt"), header = FALSE, sep = "\t")


colnames(table) <- c('cell type', 'suprema positions in ranked list', 'supremum size')
plot <- ggplot(data = table, aes(x=`suprema positions in ranked list`, y=`supremum size`, color = `cell type`)) +
  geom_point(shape = 1) +
  labs(x = 'suprema positions in ranked list', y = 'supremum size') +
  theme_classic() +
  theme(legend.position = 'none')
ggplotly(plot) %>%
  layout(
    xaxis = list(
      title = plot_axis_title_format
    ),
    yaxis = list(
      title = plot_axis_title_format
    )
  )

library(plotly)
setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/cell_types_charts")
cell_type <- "tubule"
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_",cell_type,"_ews.txt"), header = TRUE, sep = "\t")
names(table) <- c('cell type', 'rank', 'cumulative probability')
type <- filter(table, `cell type` == cell_type)
other <- filter(table, `cell type` == 'others')
plot <- plot_ly(table, x = ~rank, y=~`cumulative probability`, type = 'scatter', mode = 'lines', linetype = ~`cell type`) %>%
  layout()
plot

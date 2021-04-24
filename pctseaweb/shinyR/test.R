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


setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/cell_types_charts")
cell_type <- "tubule"
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_",cell_type,"_corr.txt"), header = TRUE, sep = "\t")
names(table)<- 'x'
plot <- plot_ly(table, x =~x, type = 'histogram') %>%
  layout()
plot


setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/cell_types_charts")
cell_type <- "tubule"
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_",cell_type,"_genes_per_cell_hist.txt"), header = TRUE, sep = "\t")
colnames(table) <- c('type', 'genes', 'cells')

plot <- plot_ly(table, x =~genes, y=~cells, type = 'bar', split=~type ) %>%
  layout()
plot


setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/global_charts")
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_ews_obs_null_hist.txt"), header = FALSE, sep = "\t", na.strings = "null")
colnames(table) <- c('Distribution', 'Enrichment_score')
tmp <- table[table$Distribution=='Observed',]
tmp2 <- hist(tmp$Enrichment_score)
max_y <- max(tmp2$counts)
plot <- plot_ly(table, alpha = 0.7, x =~Enrichment_score, type = 'histogram', split=~Distribution) %>%#, histnorm = "probability" ) %>%
  layout(
    yaxis = list(titlefont = list(size = 12), range = c(0, max_y))
  )
plot

setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/global_charts")
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_suprema_hist.txt"), header = TRUE, sep = "\t")
colnames(table) <- c('cell type', 'supremum_X')
plot <- plot_ly(table, alpha = 0.7, x =~supremum_X, type = 'histogram') %>%#, histnorm = "probability" ) %>%
  layout(

  )
plot

setwd("D:/Salva/git_projects/pctsea-parent/pctseaweb/shinyR/data/2021-03-10_18-26-27_mouse_GRIA_IP_Morpheus/global_charts")
table = fread(file = paste0("mouse_GRIA_IP_Morpheus_corr_rank_dist.txt"), header = TRUE, sep = "\t")
num_cells <- nrow(table)
names(table) <- c("rank","class","score")
plot <- plot_ly(table, x =~rank, y=~score, type = 'scatter', mode='lines') %>%
  layout()
plot

# clusters
library(morpheus)
setwd("C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/2021-04-09_17-14-40_SIMPLE_SCORE_NEGSUP_SUBTYPE")
table = fread(file = paste0("SIMPLE_SCORE_NEGSUP_SUBTYPE_correlation_genes.txt"), header = TRUE, sep = "\t")

enrichment_file <- "C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/2021-04-09_17-14-40_SIMPLE_SCORE_NEGSUP_SUBTYPE/SIMPLE_SCORE_NEGSUP_SUBTYPE_cell_types_enrichment.txt"
skip <- getLinesToSkip(enrichment_file)
table2 <- fread(file = enrichment_file, header = TRUE, skip = skip,  sep = "\t", fill = TRUE, na.strings = "NaN") # IMPORTANT: 33 is the number of rows to skip until finding the actual table on the file
significant_cell_types <- table2[`KS_p-value_BH_corrected`<0.05 & ews>0,]$cell_type

cell_types <- unique(table$cell_type)
genes <- unique(table$gene)

data <- data.frame(matrix(ncol=length(significant_cell_types)))
colnames(data) <- significant_cell_types
for(i in seq(1:nrow(table))){
  i<-1
  row <- as.character(as.vector(table[i,]))
  cell_type<-row[1]
  gene <- row[2]
  data[gene, cell_type] <- 1
  if (i==1){ # remove first row that is empty in the first iteration
    data <- data[-1,]
  }
}

data[is.na(data)]<-0
# cluster cell types:
rowv = NULL
colv = NULL

dendrogram = "both"
rowv = TRUE
colv = TRUE
# colorScheme <- list(scalingMode = "relative")
y <-
  plot <- morpheus(data,
                   Rowv = rowv,
                   Colv = colv,
                   dendrogram = dendrogram,
                   # na.rm = TRUE,
                   # columnAnnotations = y,
                   # columnGroupBy=list(list(field='TumorOrigin')),
                   # by default color scale is map to the minimum and maximum of each row independently
                   # colorScheme=colorScheme,
                   # columns=list(list(field='TumorOrigin',display=list('text_and_color')))

  )#+ scale_fill_gradient( trans = 'log' )
plot


# clusters with heatmaply
library(heatmaply)
setwd("C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/2021-04-09_17-14-40_SIMPLE_SCORE_NEGSUP_SUBTYPE")
table = fread(file = paste0("SIMPLE_SCORE_NEGSUP_SUBTYPE_correlation_genes.txt"), header = TRUE, sep = "\t")

enrichment_file <- "C:/Users/salvador/eclipse-workspace/pctsea-parent/pctseaweb/shinyR/data/2021-04-09_17-14-40_SIMPLE_SCORE_NEGSUP_SUBTYPE/SIMPLE_SCORE_NEGSUP_SUBTYPE_cell_types_enrichment.txt"
skip <- getLinesToSkip(enrichment_file)
table2 <- fread(file = enrichment_file, header = TRUE, skip = skip,  sep = "\t", fill = TRUE, na.strings = "NaN") # IMPORTANT: 33 is the number of rows to skip until finding the actual table on the file
significant_cell_types <- table2[`KS_p-value_BH_corrected`<0.05 & ews>0,]$cell_type

cell_types <- unique(table$cell_type)
genes <- unique(table$gene)

data <- data.frame(matrix(ncol=length(significant_cell_types)))
colnames(data) <- significant_cell_types
for(i in seq(1:nrow(table))){
  row <- as.character(as.vector(table[i,]))
  cell_type<-row[1]
  gene <- row[2]
  data[gene, cell_type] <- 1
  if (i==1){ # remove first row that is empty in the first iteration
    data <- data[-1,]
  }
}

data[is.na(data)]<-0
# cluster cell types:
rowv = NULL
colv = NULL

dendrogram = "both"
rowv = TRUE
colv = TRUE
heatmaply(mtcars)
  plot <- heatmaply(data)
#,
                   Rowv = rowv,
                   Colv = colv,
                   dendrogram = dendrogram,
                   # na.rm = TRUE,
                   # columnAnnotations = y,
                   # columnGroupBy=list(list(field='TumorOrigin')),
                   # by default color scale is map to the minimum and maximum of each row independently
                   # colorScheme=colorScheme,
                   # columns=list(list(field='TumorOrigin',display=list('text_and_color')))

  )#+ scale_fill_gradient( trans = 'log' )
plot

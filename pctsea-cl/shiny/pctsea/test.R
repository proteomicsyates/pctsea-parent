library(shiny)
library(dplyr)
library(stringr)
library(ggplot2)
filepath_correlations = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\input files\\sprotein\\uniprot accs\\initial_spike_new_single_cell_correlations.txt"
table = read.csv(file = filepath_correlations, header = TRUE, sep = "\t", na.strings = c("NaN"))
correlation_threshold = 0.1
table[,"positive"] <- table$Pearson.s.correlation > correlation_threshold
table[, "rank"] <- c(1:length(table[,1]))
table <- table[!is.na(table$Pearson.s.correlation),]
ggplot(data = table, 
       aes(
         x = rank,  
         y = Pearson.s.correlation, 
         group = positive,
         fill = positive)) + # color by Comprador
  labs(title = "Ranks of cells by Pearson's correlation", x = "cell #", y = "Pearson's correlation") +
  geom_line(aes(color=positive))+
  theme_classic() 


cell_type = "stromal"
filepath_correlations = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\input files\\sprotein\\uniprot accs\\initial_spike_new_TYPE_score_calculations.txt"
table = read.csv(file = filepath_correlations, header = TRUE, sep = "\t", na.strings = c("NaN"))
table <- table[table$cell_type == cell_type, ]
type <- table[table$type_or_other == 'TYPE', ]
type <- transpose(type %>% select( 3:ncol(.) ))
other <- table[table$type_or_other == 'OTHER', ]
other <- transpose(other %>% select( 3:ncol(.) ))
new_table <- data.frame(type, other)
names(new_table)[1]<-cell_type
names(new_table)[2]<-"others"
new_table[, "rank"] <- c(1:length(new_table[,1]))
new_table <- melt(data = new_table, id.vars = "rank", variable.name = "cell_type")
ggplot(data = new_table, 
       aes(
         x = rank,  
         y = value,
         group = cell_type)) + 
  labs(title = paste("Enrichment score calculation for cell type: '",cell_type, "'", sep=""), x = "cell #", y = "Cumulative Probability") +
  geom_line(aes(color = cell_type))+
  theme_classic() +
  xlim(1,max(new_table$rank)) + 
  ylim(0,1)
 

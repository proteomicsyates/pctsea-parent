library(readr)
library(ggplot2)
library(dplyr)
data <- read.csv(
  file = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\gene_distributions.txt",
  sep = "\t")
names(data) <- c("gene","cell_type","expression")
gene <- data[1,1]

data[sapply(data, is.numeric)] <- lapply(data[sapply(data, is.numeric)],
                                         as.factor)
plot <- ggplot(data=data, aes( x=expression, fill=expression ))+
  geom_bar()+
  geom_text(aes(label=..count..), stat = "count", vjust = 1.5)+
  theme(axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1))+
  labs(title=paste("Number of cells in which", gene, "has each expression"))
plot
ggplotly(plot)


data <- read.csv(
  file = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\gene_distributions.txt",
  sep = "\t")
names(data) <- c("gene","cell_type","expression")

summary_table <- data %>%
  group_by(cell_type) %>%
  summarise(count=dplyr::n(), mean=mean(expression), sd=sd(expression), min=min(expression), max=max(expression) ) %>%
  arrange(desc(count))
write.table(summary_table,
            file = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\data_from_article\\gene_distributions_stats.txt",
            sep = "\t",
            row.names = FALSE)


install.packages("mongolite")
library(mongolite)
sc <- mongo(collection ="singleCell", db = "single_cells_db", url = "mongodb://localhost:8888")
print(sc)
sc$find('{"type":"neuron"}')
ex <- mongo(collection ="expression", db = "single_cells_db", url = "mongodb://localhost:8888")
ex$info()
ex$find(query = '{"cellType":"astrocyte(bergmann glia)"}',
         limit = 10)

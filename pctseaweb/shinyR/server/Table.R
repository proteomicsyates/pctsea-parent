

createPlotWithScoreCalculation <- function(table, cell_type){
  req(table, cell_type)
  browser()
  table <- table[table$cell_type == cell_type, ]
  type <- table[table$type_or_other == 'TYPE', ]
  type <- type %>% select( 3:ncol(.) )
  type <- t(type)
  other <- table[table$type_or_other == 'OTHER', ]
  other <- other %>% select( 3:ncol(.) )
  other <- t(other)
  new_table <- data.frame(type, other)
  names(new_table)[1] <- cell_type
  names(new_table)[2] <- "others"
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
}

enrichment_file <- reactiveVal()
# select the enrichment file
observeEvent(unziped_files(),{
  browser()
  folder <- unziped_files
  folder <- paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*cell_types_enrichment.txt")[1], sep = "")
  enrichment_file(folder)
})
# read enrichment file into a table in background
enrichment_table <- eventReactive(enrichment_file(),{
  withProgress({
    setProgress(value = 0, message = "Reading enrichment table...")
    t <- fread(file = enrichment_file(), header = TRUE, skip = 33, sep = "\t")
    setProgress(value = 0.5)
    t = t[ews>0,] # take only with positives ews
    setProgress(value = 1, message = "Enrichment table read.")
    return(t)
  },
  detail = "Please wait for a few seconds..."
  )
})

# plot the table as soon as is loaded
output$enrichmentDataTable <- renderDataTable({
  enrichment_table()
},
options = list(pageLength = 10)
)

# having the table, enable us to get the unique cell types
observeEvent(enrichment_table(),{
  table <- enrichment_table()
  req(table)
  table = table[table$ews>0,]
  table <- table[!is.na(table$cell_type),]
  unique_cell_types = unique(table$cell_type)
  unique_cell_types <- c("",sort(unique_cell_types))
  updateSelectInput(session, "selectCellType",
                    choices = unique_cell_types)
})



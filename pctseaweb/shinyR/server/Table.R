
createPlotWithCorrelations <- function(table, correlation_threshold, cell_type){
  req(table)
  # create a new column that says whether the correlation pass the threshold or not
  table[,"positive"] <- table$Pearson.s.correlation > correlation_threshold
  # remove correlations that are NaN
  table <- table[!is.na(table$Pearson.s.correlation),]
  # create a rank column
  table[, "rank"] <- c(1:length(table[,1]))
  # title
  my_title <- "Ranks of cells by Pearson's correlation"
  # filter by cell type
  if(!missing(cell_type)){
    if(!sjmisc::is_empty(cell_type)){
      table <- table[table$Cell.type == cell_type,]
      my_title <- paste("Ranks of cells of type '",cell_type,  "' by Pearson's correlation", sep="")
    }else{
      return()
    }
  }
  ggplot(data = table,
         aes(
           x = rank,
           y = Pearson.s.correlation,
           group = positive,
           fill = positive)) + # color by Comprador
    labs(title = my_title, x = "cell #", y = "Pearson's correlation") +
    geom_line(aes(color=positive))+
    theme_classic()
}

createPlotWithScoreCalculation <- function(table, cell_type){
  req(cell_type)
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

# select the enrichment file
enrichment_file <- eventReactive(rv$unziped_files,{
  folder = rv$unziped_files
  paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*cell_types_enrichment.txt")[1], sep = "")
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
options = list(pageLength = 20)
)

cellTypes = reactive({
  table <- rv$enrichmentTable
  req(table)
  table = table[table$ews>0,]
  table <- table[!is.na(table$Cell.type),]
  unique_cell_types = unique(rv$enrichmentTable$Cell.type)
  unique_cell_types <- sort(unique_cell_types)
  unique_cell_types
})
observe({
  updateSelectInput(session, "selectCellType",
                    choices = cellTypes())
})

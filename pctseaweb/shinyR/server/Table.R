



enrichment_file <- reactiveVal()
# select the enrichment file
observeEvent(rv$unziped_files,{
  folder <- rv$unziped_files
  folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  files <- list.files(folder, pattern = ".*cell_types_enrichment.txt")
  if(length(files) > 0){
    file <- paste(folder, .Platform$file.sep, files[1], sep = "")
    enrichment_file(file)
  }
})
# read enrichment file into a table in background
enrichment_table <- eventReactive(enrichment_file(),{
  withProgress({
    setProgress(value = 0, message = "Reading enrichment table...")
    t <- fread(file = enrichment_file(), header = TRUE, skip = 33,  sep = "\t", fill = TRUE) # IMPORTANT: 33 is the number of rows to skip until finding the actual table on the file
    setProgress(value = 0.5)
    t = t[ews>0,] # take only with positives ews
    setProgress(value = 1, message = "Enrichment table read.")
    return(t)
  },
  detail = "Please wait for a few seconds..."
  )
})

# plot the table as soon as is loaded
output$enrichmentDataTable <- DT::renderDataTable(
  enrichment_table(),
  options = list(
    pageLength = 10,

    columnDefs = list(list(className = 'dt-center', targets = 5)),
    order = list(list(13, 'asc'), list(12, 'asc'), list(20, 'asc'))
  ),
  filter = 'top',
  selection = 'single'
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

# plot the table as soon as is loaded
output$enrichmentDataTable2 <- DT::renderDataTable(
  {
    table <- enrichment_table()
    table[, c("cell_type", "FDR", "empirical_p-value", "KS_p-value_BH_corrected")]
  },
  options = list(
    pageLength = 10,
    dom = 't',
    order = list(list(2, 'asc'), list(3, 'asc'), list(4, 'asc'))
  ),
  selection = 'single'
)



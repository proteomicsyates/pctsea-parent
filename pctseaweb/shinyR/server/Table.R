



enrichment_file <- reactiveVal()
# select the enrichment file
observeEvent(rv$unziped_files,{
  folder <- rv$unziped_files
  # folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
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
    if (!("ews" %in% colnames(t) )){
      showNotification(paste("Error reading file '",enrichment_file(),","), duration = NULL, type = "error")
      return(NULL)
    }
    # take only with positives ews:
    t = t[ews>0,]
    # replace all '_' by spaces on names
    names(t) <- gsub(x = names(t), pattern = '_', replacement = ' ')
    setProgress(value = 1, message = "Enrichment table read.")
    return(t)
  },
  detail = "Please wait for a few seconds..."
  )
})

# plot the table as soon as is loaded
output$enrichmentDataTable <- DT::renderDT(
  {
    req(enrichment_table())
    datatable(
      enrichment_table(),
      filter = 'top',
      selection = 'single',
      options = list(
        pageLength = 10,
        columnDefs = list(list(className = 'dt-center', targets = 5)),
        order = list(list(13, 'asc'), list(12, 'asc'), list(20, 'asc'))
      )
    ) %>%
      formatRound(columns=c("hyperG p-value", "FDR", "empirical p-value", "KS p-value", "KS p-value BH corrected"), digits=4) %>%
      formatRound(columns=c("log2 ratio", "ews", "2nd ews", "norm-ews", "norm-supX", "2nd supX", "Dab", "Umap x", "Umap y"), digits=2)
  }
)

# having the table, enable us to get the unique cell types
observeEvent(enrichment_table(),{
  table <- enrichment_table()
  req(table)
  table = table[table$ews>0,]
  table <- table[!is.na(table$`cell type`),]
  unique_cell_types = unique(table$`cell type`)
  unique_cell_types <- c("",sort(unique_cell_types))
  updateSelectInput(session, "selectCellType",
                    choices = unique_cell_types)
})

output$enrichmentDataTable2 <- DT::renderDT(
  {
    table <- enrichment_table()
    table <- table[, c("cell type", "empirical p-value", "FDR", "KS p-value BH corrected", "hyperG p-value")]
    datatable(
      table,
      selection = 'single',
      options = list(
        pageLength = 10,
        dom = 'lftipr',
        order = list(list(3, 'asc'), list(4, 'asc'))
      )
    ) %>%
      formatRound(columns=c("empirical p-value", "FDR", "KS p-value BH corrected", "hyperG p-value"), digits=4)
  }
)
# event that catches the selection on the table and updates the input selection of the dropdown
observeEvent(input$enrichmentDataTable2_rows_selected,{
  selected_cell_type <- input$enrichmentDataTable2_rows_selected
  table <- enrichment_table()
  req(table)
  selected_choice <- table[[selected_cell_type,"cell type"]]
  # update the dropdown menu selectCellType
  updateSelectInput(session, "selectCellType",  selected = selected_choice)
}
)

output$enrichmentDataTableForCluster <- DT::renderDT(
  {
    table <- enrichment_table()
    table <- table[, c("cell type", "empirical p-value", "FDR", "KS p-value BH corrected", "hyperG p-value")]
    datatable(
      table,
      selection = 'single',
      # rownames = FALSE,
      options = list(
        pageLength = 10,
        dom = 'lftipr',
        order = list(list(3, 'asc'), list(4, 'asc')),
        autoWidth = TRUE,
        columnDefs = list(list(width = '10px', targets = c(2,3,4,5)))
      )
    ) %>%
      formatRound(columns=c("empirical p-value", "FDR", "KS p-value BH corrected", "hyperG p-value"), digits=4)
  }
)
# # event that catches the selection on the table and highlights the charts
# observeEvent(input$enrichmentDataTableForCluster_rows_selected,{
#   selected_cell_type <- input$enrichmentDataTableForCluster_rows_selected
#   table <- enrichment_table()
#   req(table)
#   selected_choice <- table[[selected_cell_type,"cell type"]]
#
# }
# )


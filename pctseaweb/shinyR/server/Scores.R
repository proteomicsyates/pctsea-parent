createPlotWithScoreCalculation <- function(table, cell_type){
  req(table, cell_type)
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
createPlotWithScoreCalculationForCellType <- function(table, cell_type){
  req(table, cell_type)
  names(table) <- c('cell type', 'rank', 'cumulative probability')
  type <- table[table$`cell type` == cell_type, ]
  other <- table[table$`cell type` == 'others', ]

  plot <- ggplot(data = table,
                 aes(
                   x = rank,
                   y = `cumulative probability`,
                   group = `cell type`)) +
    labs(x = "cell #", y = "Cumulative Probability") +
    geom_line(aes(color = `cell type`))+
    theme_classic() +
    xlim(1,max(table$rank)) +
    ylim(0,1.00001) +
    theme(legend.title = element_blank()) # this makes it configurable by plotly on the layout
  # ggtitle(paste0("Enrich. Score calculation for: '",cell_type, "'")) +
  # theme(plot.title = element_text(size=10))
  ggplotly(plot) %>%
    layout(
      legend = list(
        orientation = "v",
        x = 0.4,
        y = 0.1,
        title = list(side = "top"),
        font = list(size = 9),
        tracegroupgap = 3
      ),
      xaxis = list(
        title = plot_axis_title_format
      ),
      yaxis = list(
        title = plot_axis_title_format
      )
    )
}



# select the scores file for the cell type
cell_type_scores_table <- eventReactive(input$selectCellType, {
  req(rv$unziped_files)
  req(input$selectCellType)
  file <- get_cell_type_file(rv$unziped_files, rv$run_name, input$selectCellType, "ews")
  if (is.null(file)){
    return()
  }
  withProgress({
    setProgress(value = 0)
    table = fread(file, header = FALSE, sep = "\t", showProgress = TRUE)
    setProgress(value = 1)
    table
  }, message = paste0("Reading score calculations from ", input$selectedCellType),
  detail = "Please wait..."
  )
})

# plot the scores plot
observeEvent(cell_type_scores_table(),{
  output$cellTypeScoreCalculationPlot <- renderPlotly(cell_type_scores_table() %>% createPlotWithScoreCalculationForCellType(., input$selectCellType))
}
)
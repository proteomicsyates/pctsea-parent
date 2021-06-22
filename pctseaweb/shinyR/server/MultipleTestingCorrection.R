createPlotMultipleTestingCorrection <- function(table){
  req(table)
  colnames(table) <- c('Distribution', 'Enrichment_score')
  obs_data <- table[table$Distribution=='Observed',]


  plot <- plot_ly(table, alpha = 0.95, x =~Enrichment_score, type = 'histogram',
                  histnorm = "probability", # so that both histograms are normalized
                  split=~Distribution)%>%#, histnorm = "probability" ) %>%
    layout(
      xaxis = list(
        showticklabels = TRUE,
        title = "Enrichment score",
        tickfont = list(size = 7),
        tickangle = '45'
      ),
      yaxis = list(titlefont = list(size = 12), #range = c(0, max_y),
                   title = "Frequency"),
      title = list(
        text = 'Distributions of Observed vs Random \nenrichment scores for FDR calculation',
        font = list(size = 11)
      ),
      legend = list(x = 0.5, y = 1)
    )

}

multiple_testing_correction_table <- reactiveVal()
observeEvent(rv$multiple_testing_correction_file, {
  req(rv$multiple_testing_correction_file)
  table = fread(rv$multiple_testing_correction_file, header = FALSE, sep = "\t", showProgress = TRUE, na.strings = "null")
  multiple_testing_correction_table(table)
})




# plot the genes per cell type histogram plot
observeEvent(multiple_testing_correction_table(),{
  output$globalMultipleTestingCorrectionPlot <- renderPlotly(multiple_testing_correction_table() %>% createPlotMultipleTestingCorrection())
})


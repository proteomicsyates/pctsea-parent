createPlotMultipleTestingCorrection <- function(table){
  req(table)
  colnames(table) <- c('Distribution', 'Enrichment score')
  plot <- ggplot(data=table, aes(x=`Enrichment score`, fill=Distribution, color=Distribution)) +
    geom_density(alpha=0.3) +
    theme_classic()+
    theme(plot.title = element_text(size=10)) +
    theme(legend.title = element_blank())
 
  ggplotly(plot) %>%
    layout(
      legend = list(
        orientation = "h",
        x = 0.3,
        y = 1.08,
        title = NULL
      ),
      xaxis = list(
        showticklabels = TRUE,
        title = plot_axis_title_format,
        tickfont = list(size = 7),
        tickangle = '45'
        ),
      yaxis = list(
        title = plot_axis_title_format
      ),
      title = list(
        text = 'FDR calculation',
        font = list(size = 11)
      )
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


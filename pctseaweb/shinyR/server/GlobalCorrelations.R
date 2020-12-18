

createPlotWithGlobalCorrelationsNEW <- function(table){
  req(table)
  names(table) <- c('x', 'Pearson\'s correlation', 'Frequency (# cells)')
  plot <- ggplot(data = table,
                 aes(
                   x = `Pearson\'s correlation`,
                   y = `Frequency (# cells)`)) +
    labs(x = "Pearson\'s correlation", y = "Frequency (# cells)") +
    geom_line(aes(color = x))+
    theme_classic() +
    xlim(-1,1)
  # ggtitle(paste0("Corr. distrib. for: '",cell_type, "'")) +
  # theme(plot.title = element_text(size=10))
  ggplotly(plot) %>% layout(showlegend = FALSE)
}
createPlotWithGlobalRankCorrelationsNEW <- function(table){
  req(table)
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
}

global_correlations_table <- reactiveVal()
observeEvent(rv$global_correlations_file, {
  req(rv$global_correlations_file)
  table = fread(rv$global_correlations_file, header = FALSE, sep = "\t", showProgress = TRUE)
  global_correlations_table(table)
})
global_correlations_rank_table <- reactiveVal()
observeEvent(rv$global_correlations_rank_file, {
  req(rv$global_correlations_rank_file)
  table = fread(rv$global_correlations_rank_file, header = FALSE, sep = "\t", showProgress = TRUE)
  global_correlations_rank_table(table)
})


# plot the global correlation histogram plot
observeEvent(global_correlations_table(),{
  output$globalCorrelationsPlot <- renderPlotly(global_correlations_table() %>% createPlotWithGlobalCorrelationsNEW(.))
})


# plot the global rank correlation histogram plot
observeEvent(global_correlations_rank_table(),{
  output$globalCorrelationsRankPlot <- renderPlotly(global_correlations_rank_table() %>% createPlotWithGlobalRankCorrelationsNEW(.))
})



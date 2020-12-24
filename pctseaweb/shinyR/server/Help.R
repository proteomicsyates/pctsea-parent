# button helps
observeEvent(input$globalCorrelationsPlotHelp, {
  help = getChartHelpText('globalCorrelationsPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$globalCorrelationsRankPlotHelp, {
  help = getChartHelpText('globalCorrelationsRankPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$globalGenesPerCellTypePlotHelp, {
  help = getChartHelpText('globalGenesPerCellTypePlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$globalMultipleTestingCorrectionPlotHelp, {
  help = getChartHelpText('globalMultipleTestingCorrectionPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$globalSupremaHistogramPlotHelp, {
  help = getChartHelpText('globalSupremaHistogramPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$globalSupremaScatterPlotHelp, {
  help = getChartHelpText('globalSupremaScatterPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$cellTypeCorrelationsPlotHelp, {
  help = getChartHelpText('cellTypeCorrelationsPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$cellTypeScoreCalculationPlotHelp, {
  help = getChartHelpText('cellTypeScoreCalculationPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$genesPerCellTypePlotHelp, {
  help = getChartHelpText('genesPerCellTypePlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})

observeEvent(input$umapAllPlotHelp, {
  help = getChartHelpText('umapAllPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$umapHypGPlotHelp, {
  help = getChartHelpText('umapHypGPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$umapKSTestPlotHelp, {
  help = getChartHelpText('umapKSTestPlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$umapSig001PlotHelp, {
  help = getChartHelpText('umapSig001PlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
observeEvent(input$umapSig005PlotHelp, {
  help = getChartHelpText('umapSig005PlotHelp')
  showModal(modalDialog(
    title = help['title'],
    help['text'],
    easyClose = TRUE,
    footer = NULL
  ))
})
getChartHelpText <- function(key){
  ret<-c('title' = key, 'text' = key)
  if (key == 'globalCorrelationsPlotHelp'){
    ret <- c('title' = 'Global correlations distribution', 'text' = 'text about this chart')
  }else if (key == 'globalCorrelationsRankPlotHelp'){
    
  }else if (key == 'globalGenesPerCellTypePlotHelp'){
  }else if (key == 'globalMultipleTestingCorrectionPlotHelp'){
  }else if (key == 'globalSupremaHistogramPlotHelp'){
  }else if (key == 'globalSupremaScatterPlotHelp'){
  }else if (key == 'cellTypeCorrelationsPlotHelp'){
  }else if (key == 'cellTypeScoreCalculationPlotHelp'){
  }else if (key == 'genesPerCellTypePlotHelp'){
  }else if (key == 'umapAllPlotHelp'){
  }else if (key == 'umapHypGPlotHelp'){
  }else if (key == 'umapKSTestPlotHelp'){
  }else if (key == 'umapSig001PlotHelp'){
  }else if (key == 'umapSig005PlotHelp'){
  }
  return(ret)
}
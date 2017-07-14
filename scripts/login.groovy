def message
if (req.getParameter('login') == 'my-name' && req.getParameter('pass') == 'my-pass') {
    message = 'successful'
} else {
    message = 'fail'
}

return """\
<response>
    <message>${message}</message>
</response>
"""
